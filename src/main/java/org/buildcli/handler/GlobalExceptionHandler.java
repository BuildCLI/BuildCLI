package org.buildcli.handler;

import org.buildcli.exceptions.ConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // مدیریت استثناهای عمومی
    public static ErrorResponse handleException(Exception ex) {
        ErrorResponse errorResponse;

        if (ex instanceof ConfigException) {
            errorResponse = handleConfigException((ConfigException) ex);
        } else if (ex instanceof IllegalArgumentException) {
            errorResponse = handleIllegalArgumentException((IllegalArgumentException) ex);
        } else if (ex instanceof IOException) {
            errorResponse = handleIOException((IOException) ex);
        } else if (ex instanceof NullPointerException) {
            errorResponse = handleNullPointerException((NullPointerException) ex);
        } else if (ex instanceof IndexOutOfBoundsException) {
            errorResponse = handleIndexOutOfBoundsException((IndexOutOfBoundsException) ex);
        } else if (ex instanceof RuntimeException) {
            errorResponse = handleRuntimeException((RuntimeException) ex);
        } else {
            errorResponse = handleGenericException(ex);
        }

        logger.error(errorResponse.toString()); // لاگ کردن پاسخ خطا
        return errorResponse;
    }

    private static ErrorResponse handleConfigException(ConfigException ex) {
        String message = "Configuration error: " + ex.getMessage();
        logger.error(message);
        return new ErrorResponse(message, "Please check your configuration file."); // جزئیات به جای null
    }

    private static ErrorResponse handleIllegalArgumentException(IllegalArgumentException ex) {
        String message = "Invalid argument: " + ex.getMessage();
        logger.error(message);
        return new ErrorResponse(message, "Ensure the argument is valid."); // جزئیات به جای null
    }

    private static ErrorResponse handleIOException(IOException ex) {
        String message = "I/O error: " + ex.getMessage();
        logger.error(message);
        return new ErrorResponse(message, "Check file paths and permissions."); // جزئیات به جای null
    }

    private static ErrorResponse handleNullPointerException(NullPointerException ex) {
        String message = "Null pointer error: " + ex.getMessage();
        logger.error(message);
        return new ErrorResponse(message, "Check for null values in your code."); // جزئیات به جای null
    }

    private static ErrorResponse handleIndexOutOfBoundsException(IndexOutOfBoundsException ex) {
        String message = "Index out of bounds: " + ex.getMessage();
        logger.error(message);
        return new ErrorResponse(message, "Check your index values."); // جزئیات به جای null
    }

    private static ErrorResponse handleRuntimeException(RuntimeException ex) {
        String message = "Runtime error: " + ex.getMessage();
        logger.error(message);
        return new ErrorResponse(message, "An unexpected runtime error occurred."); // جزئیات به جای null
    }

    private static ErrorResponse handleGenericException(Exception ex) {
        String message = "An unexpected error occurred: " + ex.getMessage();
        logger.error(message);
        return new ErrorResponse(message, "Please contact support."); // جزئیات به جای null
    }
}