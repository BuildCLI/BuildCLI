package dev.buildcli.core.domain.maven;

import io.github.dumijdev.dpxml.stereotype.Pojolizable;
import io.github.dumijdev.dpxml.stereotype.Xmlizable;

@Xmlizable
@Pojolizable
public class Repository {
  private String id;
  private String url;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }
}
