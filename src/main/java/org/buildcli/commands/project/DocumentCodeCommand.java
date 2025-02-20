package org.buildcli.commands.project;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.buildcli.domain.BuildCLICommand;
import org.buildcli.handler.GlobalExceptionHandler;
import org.buildcli.utils.CodeDocumenter;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(
    name = "document-code",
    aliases = {"docs"},
    description = "Generates documentation for the project code. This command scans the specified files and extracts structured documentation.",
    mixinStandardHelpOptions = true
)
public class DocumentCodeCommand implements BuildCLICommand {
  private final ExecutorService executorService = Executors.newCachedThreadPool();

  @Parameters
  private List<String> files;

  @Override
  public void run() {
    try{
    if (files == null || files.isEmpty()) {
      return;
    }

    var execsAsync = new CompletableFuture[files.size()];

    for (int i = 0; i < files.size(); i++) {
      final var finalI = i;
      execsAsync[i] = CompletableFuture.runAsync(() -> {
        CodeDocumenter.getDocumentationFromOllama(files.get(finalI));
      }, executorService);
    }
    CompletableFuture.allOf(execsAsync).join();
    } catch (Exception e) {
      GlobalExceptionHandler.handleException(e);
    }
  }
}
