package dev.buildcli.core.exceptions;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConfigExceptionTest {

    @Test
    void testConfigExceptionWithMessageAndCause() {
        // Criando a causa (outra exceção)
        Throwable cause = new Throwable("Causa da exceção");

        // Criando a exceção com uma mensagem e uma causa
        ConfigException exception = new ConfigException("Mensagem de erro", cause);

        // Verificando se a mensagem e a causa estão corretas
        assertEquals("Mensagem de erro", exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testConfigExceptionWithMessage() {
        // Criando a exceção com apenas uma mensagem
        ConfigException exception = new ConfigException("Mensagem de erro");

        // Verificando se a mensagem está correta
        assertEquals("Mensagem de erro", exception.getMessage());
        assertNull(exception.getCause());
    }
}
