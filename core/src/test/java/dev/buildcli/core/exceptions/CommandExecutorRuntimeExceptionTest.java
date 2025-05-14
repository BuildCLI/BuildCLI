package dev.buildcli.core.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CommandExecutorRuntimeExceptionTest {

    private static final String ERROR_MESSAGE = "Error message";
    private static final String CAUSE = "Cause exception";

    @Test
    @DisplayName("Testa a CommandExecutorRuntimeException sem parâmetros")
    public void testCommandExecutorRuntimeExceptionWithoutParams() {
        CommandExecutorRuntimeException exception = new CommandExecutorRuntimeException();

        assertNull(exception.getCause());
        assertNull(exception.getMessage());
    }

    @Test
    @DisplayName("Testa a mensagem de CommandExecutorRuntimeException está correta")
    public void testCommandExecutorRuntimeExceptionWithMessage() {
        CommandExecutorRuntimeException exception = new CommandExecutorRuntimeException(ERROR_MESSAGE);

        assertEquals(ERROR_MESSAGE, exception.getMessage());
    }

    @Test
    @DisplayName("Testa a causa de CommandExecutorRuntimeException está correta")
    public void testCommandExecutorRuntimeExceptionWithCause() {
        Throwable cause_exception = new Throwable(CAUSE);

        CommandExecutorRuntimeException exception = new CommandExecutorRuntimeException(cause_exception);

        assertEquals(cause_exception, exception.getCause());
    }

    @Test
    @DisplayName("Testa a causa e mensagem de CommandExecutorRuntimeException está correto")
    public void testCommandExecutorRuntimeExceptionWithCauseAndMessage() {
        Throwable cause_exception = new Throwable(CAUSE);

        CommandExecutorRuntimeException exception = new CommandExecutorRuntimeException(ERROR_MESSAGE, cause_exception);

        assertEquals(cause_exception, exception.getCause());
        assertEquals(ERROR_MESSAGE, exception.getMessage());
    }

    @Test
    @DisplayName("Testa a mensagem e a causa do construtor completo")
    public void testCommandExecutorRuntimeExceptionWithAllParams() {
        Throwable cause_exception = new Throwable(CAUSE);

        CommandExecutorRuntimeException exception = new CommandExecutorRuntimeException(ERROR_MESSAGE,
                cause_exception,
                true,
                true
        );

        assertEquals(cause_exception, exception.getCause());
        assertEquals(ERROR_MESSAGE, exception.getMessage());
    }
}
