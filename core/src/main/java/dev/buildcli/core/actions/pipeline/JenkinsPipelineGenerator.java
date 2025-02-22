package dev.buildcli.core.actions.pipeline;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

class JenkinsPipelineGenerator implements PipelineFileGenerator {
  @Override
  public void generate() throws IOException {
    File jenkinsFile = new File("Jenkinsfile");
    try (FileWriter writer = new FileWriter(jenkinsFile)) {
      writer.write("""
          pipeline {
              agent any
          
              stages {
                  stage('Build') {
                      steps {
                          sh 'mvn clean install'
                      }
                  }
                  stage('Test') {
                      steps {
                          sh 'mvn test'
                      }
                  }
              }
          }
          """);
    }
  }
}
