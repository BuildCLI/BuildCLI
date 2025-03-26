package dev.buildcli.plugin.enums;

public enum TemplateType {
  PIPELINE("CI/CD Pipeline"),
  PROJECT("Project Scaffold"),
  DOCKER("Docker/Generic Container Setup"),
  KUBERNETES("Kubernetes Resources"),
  QUICKSTART("Quickstart"),;

  private final String label;

  TemplateType(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }
}
