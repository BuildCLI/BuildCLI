package dev.buildcli.core.domain.jar;

import java.io.File;

public class Jar {
  private final File file;
  private final long size;

  public Jar(File file) {
    if (file == null) throw new NullPointerException("file is null");
    else if (file.isDirectory()) throw new IllegalArgumentException(file.getAbsolutePath() + " is a directory");
    else if (!file.getName().endsWith(".jar")) throw new IllegalArgumentException(file.getAbsolutePath() + " is not a jar file");

    this.file = file;

    this.size = file.length();
  }

  public File getFile() {
    return file;
  }

  public long getSize() {
    return size;
  }
}
