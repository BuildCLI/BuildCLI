package dev.buildcli.core.actions.pipeline;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PipelineFileGeneratorFactoryTest {

  @Test
  void shouldReturnGithubActionsPipelineGenerator() {
    PipelineFileGenerator generator = PipelineFileGenerator.PipelineFileGeneratorFactory.factory("github");

    Assertions.assertInstanceOf(GithubActionsPipelineGenerator.class, generator);
  }

  @Test
  void shouldReturnJenkinsPipelineGenerator() {
    PipelineFileGenerator generator = PipelineFileGenerator.PipelineFileGeneratorFactory.factory("jenkins");

    Assertions.assertInstanceOf(JenkinsPipelineGenerator.class, generator);
  }

  @Test
  void shouldReturnGitlabPipelineGenerator() {
    PipelineFileGenerator generator = PipelineFileGenerator.PipelineFileGeneratorFactory.factory("gitlab");

    Assertions.assertInstanceOf(GitlabPipelineGenerator.class, generator);
  }

  @Test
  void shouldThrowExceptionWhenPlatformIsUnknown() {
    IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class,
        () -> PipelineFileGenerator.PipelineFileGeneratorFactory.factory("unknown"));

    Assertions.assertEquals("Unexpected value: unknown", exception.getMessage());
  }
}
