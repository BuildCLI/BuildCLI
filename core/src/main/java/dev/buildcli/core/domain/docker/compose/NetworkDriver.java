package dev.buildcli.core.domain.docker.compose;

public enum NetworkDriver {
  BRIDGE,
  HOST,
  OVERLAY,
  MACVLAN,
  NONE,
  IPVLAN;


  @Override
  public String toString() {
    return name().toLowerCase();
  }
}
