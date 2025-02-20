package org.buildcli.handler;

import org.buildcli.exceptions.ConfigException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GlobalExceptionHandlerTest {

    @Test
    public void testHandleConfigException() {
        ConfigException exception = new ConfigException("Invalid configuration");
        ErrorResponse response = GlobalExceptionHandler.handleException(exception);

        assertEquals("Configuration error: Invalid configuration", response.getMessage());
    }

    @Test
    public void testHandleIllegalArgumentException() {
        IllegalArgumentException exception = new IllegalArgumentException("Invalid argument");
        ErrorResponse response = GlobalExceptionHandler.handleException(exception);

        assertEquals("Invalid argument: Invalid argument", response.getMessage());
    }

    @Test
    public void testHandleIOException() {
        IOException exception = new IOException("File not found");
        ErrorResponse response = GlobalExceptionHandler.handleException(exception);

        assertEquals("I/O error: File not found", response.getMessage());
    }

    @Test
    public void testHandleNullPointerException() {
        NullPointerException exception = new NullPointerException("Null value");
        ErrorResponse response = GlobalExceptionHandler.handleException(exception);

        assertEquals("Null pointer error: Null value", response.getMessage());
    }

    @Test
    public void testHandleIndexOutOfBoundsException() {
        IndexOutOfBoundsException exception = new IndexOutOfBoundsException("Index out of range");
        ErrorResponse response = GlobalExceptionHandler.handleException(exception);

        assertEquals("Index out of bounds: Index out of range", response.getMessage());
    }

    @Test
    public void testHandleRuntimeException() {
        RuntimeException exception = new RuntimeException("Runtime error occurred");
        ErrorResponse response = GlobalExceptionHandler.handleException(exception);

        assertEquals("Runtime error: Runtime error occurred", response.getMessage());
    }

    @Test
    public void testHandleGenericException() {
        Exception exception = new Exception("Unexpected error");
        ErrorResponse response = GlobalExceptionHandler.handleException(exception);

        assertEquals("An unexpected error occurred: Unexpected error", response.getMessage());
    }
}