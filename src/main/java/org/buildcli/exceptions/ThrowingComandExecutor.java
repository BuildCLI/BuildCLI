package org.buildcli.exceptions;

import java.io.IOException;

@Deprecated( forRemoval = true)
@FunctionalInterface
public interface ThrowingComandExecutor {
	void exec() throws IOException;
}
