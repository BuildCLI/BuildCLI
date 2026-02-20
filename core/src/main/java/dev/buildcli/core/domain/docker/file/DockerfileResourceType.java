package dev.buildcli.core.domain.docker.file;

public enum DockerfileResourceType {
  FROM, WORKDIR, RUN, EXPOSE, ENV, ENTRYPOINT, ARG, COPY, ADD, NONE, CMD
}
