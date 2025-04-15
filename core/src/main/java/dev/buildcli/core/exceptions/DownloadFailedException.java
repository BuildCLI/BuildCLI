package dev.buildcli.core.exceptions;

import java.io.IOException;

public class DownloadFailedException extends RuntimeException {
  public DownloadFailedException(String message) {
    super(message);
  }
}
