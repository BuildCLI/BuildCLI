package dev.buildcli.infrastructure;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * One JDBC connection to the state database, shared by everything that reads and writes it. With {@code batching}, writes
 * return at once and one writer thread applies them in groups, one transaction each: what piles up while a group is being
 * committed becomes the next group. Reads wait for what was written before them. A failed write is reported by the next
 * write and by {@link #close()}.
 */
final class StateDb implements AutoCloseable {
    private static final int QUEUE_CAPACITY = 100_000;
    private static final int MAX_BATCH = 2_000;

    interface Binder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    private record Op(String sql, Binder binder) {}

    private record Fence(java.util.concurrent.CountDownLatch done) {}

    final boolean h2;
    private final Connection connection;
    /** Guards the connection: the writer thread and the readers take turns. */
    private final Object db = new Object();
    private final java.util.concurrent.BlockingQueue<Object> queue;
    private final Thread writer;
    private volatile Throwable failure;
    /** Writes are few kinds of statement repeated many times: prepare each once. Only used while holding {@link #db}. */
    private final java.util.Map<String, PreparedStatement> statements = new java.util.HashMap<>();

    StateDb(String jdbcUrl, boolean batching) {
        this.h2 = jdbcUrl.startsWith("jdbc:h2:");
        Connection opened;
        try {
            opened = DriverManager.getConnection(jdbcUrl);
        } catch (SQLException e) {
            throw new IllegalStateException(openFailure(jdbcUrl, e), e);
        }
        try {
            if (!h2) {
                try (Statement st = opened.createStatement()) {
                    st.execute("PRAGMA journal_mode=WAL");
                    st.execute("PRAGMA synchronous=NORMAL");
                    st.execute("PRAGMA busy_timeout=5000");
                }
            }
            StateSchema.migrate(opened, h2);
        } catch (SQLException | RuntimeException e) {
            // A refused or broken database must not leave its file locked: on Windows an open handle blocks deleting it.
            try {
                opened.close();
            } catch (SQLException ignored) {
                // nothing more can be done; the original failure is what matters
            }
            if (e instanceof IllegalStateException ise) {
                throw ise;
            }
            throw new IllegalStateException(openFailure(jdbcUrl, e), e);
        }
        this.connection = opened;
        if (batching) {
            this.queue = new java.util.concurrent.LinkedBlockingQueue<>(QUEUE_CAPACITY);
            // a platform thread: JDBC blocks inside native code and monitors, which a virtual thread would pin
            this.writer = new Thread(this::writeLoop, "state-writer");
            this.writer.setDaemon(true);
            this.writer.start();
        } else {
            this.queue = null;
            this.writer = null;
        }
    }

    private static String openFailure(String jdbcUrl, Exception e) {
        String m = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        if (jdbcUrl.startsWith("jdbc:h2:file") && (m.contains("already in use") || m.contains("Locked by another process"))) {
            return "the H2 state database is open in another BuildCLI, and H2 lets one process use it at a time"
                    + " (SQLite, the default, can be read from several): " + m;
        }
        return "cannot open the state database " + jdbcUrl + ": " + m;
    }

    int schemaVersion() {
        flush();
        synchronized (db) {
            try {
                return StateSchema.readVersion(connection, h2);
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @Override
    public void close() throws SQLException {
        if (writer != null) {
            flush();
            writer.interrupt();
            try {
                writer.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        synchronized (db) {
            for (PreparedStatement ps : statements.values()) {
                try {
                    ps.close();
                } catch (SQLException ignored) {
                    // the connection is closed right after
                }
            }
            statements.clear();
            connection.close();
        }
        Throwable lost = failure;
        if (lost != null) {
            failure = null;
            throw new SQLException("some writes to the state database failed: " + lost.getMessage(), lost);
        }
    }

    /** Applies a write now, or queues it for the writer thread. */
    void write(String sql, Binder binder) {
        if (queue == null) {
            synchronized (db) {
                execute(sql, binder);
            }
            return;
        }
        Throwable earlier = failure;
        if (earlier != null) {
            failure = null;
            throw new IllegalStateException("an earlier write to the state database failed: " + earlier.getMessage(), earlier);
        }
        try {
            queue.put(new Op(sql, binder)); // blocks when 100000 writes are waiting, instead of growing without bound
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while writing to the state database", e);
        }
    }

    private void execute(String sql, Binder binder) {
        try {
            PreparedStatement ps = statements.get(sql);
            if (ps == null) {
                ps = connection.prepareStatement(sql);
                statements.put(sql, ps);
            }
            ps.clearParameters();
            binder.bind(ps);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    /** Waits until everything written before this call has been applied. */
    void flush() {
        if (queue == null || !writer.isAlive()) {
            return;
        }
        var fence = new Fence(new java.util.concurrent.CountDownLatch(1));
        try {
            queue.put(fence);
            fence.done().await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void writeLoop() {
        List<Object> batch = new ArrayList<>();
        while (true) {
            try {
                batch.add(queue.take());
            } catch (InterruptedException e) {
                // closing: apply what is still waiting, then stop
                queue.drainTo(batch);
                apply(batch);
                Thread.currentThread().interrupt();
                return;
            }
            queue.drainTo(batch, MAX_BATCH - 1);
            apply(batch);
            batch.clear();
        }
    }

    /** One transaction for the whole group; a write that fails is remembered and does not stop the others. */
    private void apply(List<Object> batch) {
        synchronized (db) {
            try {
                connection.setAutoCommit(false);
                for (Object o : batch) {
                    if (o instanceof Op op) {
                        try {
                            execute(op.sql(), op.binder());
                        } catch (RuntimeException e) {
                            failure = e;
                        }
                    }
                }
                connection.commit();
            } catch (SQLException e) {
                failure = e;
            } finally {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException ignored) {
                    // the connection is closing; nothing more to do
                }
            }
        }
        for (Object o : batch) {
            if (o instanceof Fence f) {
                f.done().countDown();
            }
        }
        batch.clear();
    }

    <T> List<T> query(String sql, Binder binder, RowMapper<T> mapper) {
        flush();
        List<T> out = new ArrayList<>();
        synchronized (db) {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                binder.bind(ps);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        out.add(mapper.map(rs));
                    }
                }
            } catch (SQLException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        }
        return out;
    }
}
