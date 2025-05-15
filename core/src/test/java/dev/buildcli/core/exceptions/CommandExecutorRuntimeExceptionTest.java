package dev.buildcli.core.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CommandExecutorRuntimeExceptionTest {

    /**
    * Default error message used for testing CommandExecutorRuntimeException.
    */
    private static final String ERROR_MESSAGE = "Error message";

    /**
     * Default cause message used for testing CommandExecutorRuntimeException.
     */
    private static final String CAUSE_EXCEPTION = "Cause exception";

    @Test
    @DisplayName("Testa a CommandExecutorRuntimeException sem parâmetros")
    public void testCommandExecutorRuntimeExceptionWithoutParams() {
        CommandExecutorRuntimeException exception =
                new CommandExecutorRuntimeException();

        assertNull(exception.getCause());
        assertNull(exception.getMessage());
    }

    @Test
    @DisplayName("Testa a mensagem de CommandExecutorRuntimeException está correta")
    public void testCommandExecutorRuntimeExceptionWithMessage() {
        CommandExecutorRuntimeException exception =
                new CommandExecutorRuntimeException(ERROR_MESSAGE);

        assertEquals(ERROR_MESSAGE, exception.getMessage());
    }

    @Test
    @DisplayName("Testa a causa de CommandExecutorRuntimeException está correta")
    public void testCommandExecutorRuntimeExceptionWithCause() {
        Throwable causeException = new Throwable(CAUSE_EXCEPTION);

        CommandExecutorRuntimeException exception =
                new CommandExecutorRuntimeException(causeException);

        assertEquals(causeException, exception.getCause());
    }

    @Test
    @DisplayName("Testa a causa e mensagem de CommandExecutorRuntimeException")
    public void testCommandExecutorRuntimeExceptionWithCauseAndMessage() {
        Throwable causeException = new Throwable(CAUSE_EXCEPTION);

        CommandExecutorRuntimeException exception =
                new CommandExecutorRuntimeException(ERROR_MESSAGE,
                        causeException);

        assertEquals(causeException, exception.getCause());
        assertEquals(ERROR_MESSAGE, exception.getMessage());
    }

    @Test
    @DisplayName("Testa a mensagem e a causa do construtor completo")
    public void testCommandExecutorRuntimeExceptionWithAllParams() {
        Throwable causeException = new Throwable(CAUSE_EXCEPTION);

        CommandExecutorRuntimeException exception =
                new CommandExecutorRuntimeException(
                        ERROR_MESSAGE,
                        causeException,
                        true,
                        true
        );

        assertEquals(causeException, exception.getCause());
        assertEquals(ERROR_MESSAGE, exception.getMessage());
    }
}
