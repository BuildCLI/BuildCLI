package dev.buildcli.core.utils.tools.maven;

import io.github.dumijdev.dpxml.model.Node;
import io.github.dumijdev.dpxml.parser.impl.xml.DefaultXmlizer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public abstract class PomWriter {
  public static void write(Node project) {
    try {
      if (!"project".equals(project.name())) {
        throw new IllegalStateException("project is not a project");
      }

      var pom = new File("pom.xml");

      var pomString = new DefaultXmlizer().xmlify(project);

      Files.write(pom.toPath(), pomString.getBytes(StandardCharsets.UTF_8));

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
