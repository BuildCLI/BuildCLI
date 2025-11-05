package dev.buildcli.core.exceptions;

public class DockerException extends Exception {
    public DockerException(String message) {
        super(message);
    }

    public DockerException(String message, Throwable cause) {
        super(message, cause);
    }

    public static class DockerBuildException extends DockerException {
        public DockerBuildException(String message) {
            super(message);
        }

        public DockerBuildException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class DockerComposeFileNotFoundException extends DockerException {
        public DockerComposeFileNotFoundException(String message) {
            super(message);
        }

        public DockerComposeFileNotFoundException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class DockerEngineNotRunningException extends DockerException {
        public DockerEngineNotRunningException(String message) {
            super(message);
        }

        public DockerEngineNotRunningException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class NoRunningContainersException extends DockerException {
        public NoRunningContainersException(String message) {
            super(message);
        }

        public NoRunningContainersException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class DockerfileNotFoundException extends DockerException {
        public DockerfileNotFoundException(String message) {
            super(message);
        }

        public DockerfileNotFoundException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}