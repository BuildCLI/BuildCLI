package dev.buildcli.core.exceptions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ExtractionRuntimeExceptionTest {

  public static final String ERROR_MESSAGE = "Error message";
  public static final String EXCEPTION_CAUSE = "Exception cause";

  @Test
  void testExtractRuntimeExceptionWithMessageAndCause() {
    Throwable cause = new Throwable(EXCEPTION_CAUSE);

    ExtractionRuntimeException exception = new ExtractionRuntimeException(ERROR_MESSAGE, cause);

    assertEquals(cause, exception.getCause());
    assertThat(ERROR_MESSAGE, equalTo(exception.getMessage()));
  }

  @Test
  void testExtractRuntimeExceptionWithMessage() {
    ExtractionRuntimeException exception = new ExtractionRuntimeException(ERROR_MESSAGE);

    assertNull(exception.getCause());
    assertThat(ERROR_MESSAGE, equalTo(exception.getMessage()));
  }

  @Test
  void testExtractRuntimeExceptionWithThrowable() {
    Throwable cause = new Throwable(EXCEPTION_CAUSE);

    ExtractionRuntimeException exception = new ExtractionRuntimeException(cause);

    assertNotNull(exception.getCause());
    assertThat(exception.getCause(), equalTo(cause));
    assertThat(EXCEPTION_CAUSE, equalTo(exception.getCause().getMessage()));
  }

  @Test
  void testExtractRuntimeExceptionWithAllParameters() {
    Throwable cause = new Throwable(EXCEPTION_CAUSE);

    ExtractionRuntimeException exception = new ExtractionRuntimeException(ERROR_MESSAGE, cause, true, true);

    assertNotNull(exception.getCause());
    assertThat(exception.getCause(), equalTo(cause));
    assertThat(ERROR_MESSAGE, equalTo(exception.getMessage()));
  }

  @Test
  void testExtractRuntimeExceptionWithoutParameters() {
    ExtractionRuntimeException exception = new ExtractionRuntimeException();

    assertNull(exception.getMessage());
    assertNull(exception.getCause());
  }
}