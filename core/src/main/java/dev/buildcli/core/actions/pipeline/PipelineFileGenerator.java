package dev.buildcli.core.actions.pipeline;

import java.io.IOException;

public interface PipelineFileGenerator {
  void generate() throws IOException;

  interface PipelineFileGeneratorFactory {
    static PipelineFileGenerator factory(String plaftorm) {
      return switch (plaftorm) {
        case "github" -> new GithubActionsPipelineGenerator();
        case "jenkins" -> new JenkinsPipelineGenerator();
        case "gitlab" -> new GitlabPipelineGenerator();
        default -> throw new IllegalStateException("Unexpected value: " + plaftorm);
      };
    }
  }
}
