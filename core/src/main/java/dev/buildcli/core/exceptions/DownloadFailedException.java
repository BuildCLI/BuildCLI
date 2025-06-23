package dev.buildcli.core.exceptions;

public class DownloadFailedException extends RuntimeException {
  public DownloadFailedException(String message) {
    super(message);
  }
}
