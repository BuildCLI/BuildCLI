package dev.buildcli.core.utils.filesystem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class FindFilesUtils {
  private FindFilesUtils() {
  }

  public static List<File> search(File dir, String...extensions) {
    List<File> files = new ArrayList<>();
    if (dir.isDirectory()) {
      var listFiles = dir.listFiles();
      if (listFiles == null || listFiles.length == 0) {
        return files;
      }

      for (File file : listFiles) {
        if (file.isDirectory()) {
          files.addAll(search(file, extensions));
          continue;
        }

        if (endsWith(file, extensions)) {
          files.add(file);
        }
      }
    } else if (dir.isFile() && endsWith(dir, extensions)) {
      files.add(dir);
    }

    return List.copyOf(files);
  }

  public static List<File> searchJarFiles(File dir) {
    return search(dir, ".jar");
  }

  public static List<File> searchJavaFiles(File dir) {
    final String[] extensions = { ".java", ".kt", ".groovy", ".scala" };
    return search(dir, extensions);
  }

  public static List<File> searchCodeFiles(File dir) {
    final String[] extensions = {".java", ".kt", ".scala", ".js", ".ts", ".jsx", ".tsx"};
    return search(dir, extensions);
  }

  private static boolean endsWith(File file, String...suffixes) {
    for (String suffix : suffixes) {
      if (file.getName().endsWith(suffix)) {
        return true;
      }
    }

    return false;
  }
}
