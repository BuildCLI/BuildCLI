package dev.buildcli.core.domain.docker.compose;

public enum VolumeDriver {
  LOCAL,
  NFS,
  BIND,
  VOLUME,
  TMPFS,
  NPIPE,
  NAMED;

  @Override
  public String toString() {
    return name().toLowerCase();
  }
}
