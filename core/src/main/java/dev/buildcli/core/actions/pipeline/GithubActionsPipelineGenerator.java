package dev.buildcli.core.actions.pipeline;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

class GithubActionsPipelineGenerator implements PipelineFileGenerator {
  @Override
  public void generate() throws IOException {
    File workflowDir = new File(".github/workflows");
    if (!workflowDir.exists() && !workflowDir.mkdirs()) {
      throw new IOException("Failed to create .github/workflows directory.");
    }

    File workflowFile = new File(workflowDir, "ci.yml");
    try (FileWriter writer = new FileWriter(workflowFile)) {
      writer.write("""
          name: Build and Test
          
          on:
            push:
              branches:
                - main
            pull_request:
              branches:
                - main
          
          jobs:
            build:
              runs-on: ubuntu-latest
          
              steps:
                - name: Checkout code
                  uses: actions/checkout@v3
          
                - name: Set up JDK 17
                  uses: actions/setup-java@v3
                  with:
                    java-version: '17'
          
                - name: Build with Maven
                  run: mvn clean install
          
                - name: Run tests
                  run: mvn test
          """);
    }
  }
}
