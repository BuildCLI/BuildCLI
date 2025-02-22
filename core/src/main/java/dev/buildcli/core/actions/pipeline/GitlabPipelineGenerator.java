package dev.buildcli.core.actions.pipeline;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

class GitlabPipelineGenerator implements PipelineFileGenerator {
  @Override
  public void generate() throws IOException {
    File gitlabCIFile = new File(".gitlab-ci.yml");
    try (FileWriter writer = new FileWriter(gitlabCIFile)) {
      writer.write("""
                stages:
                  - build
                  - test

                build:
                  stage: build
                  script:
                    - mvn clean install

                test:
                  stage: test
                  script:
                    - mvn test
                """);
    }
  }
}
