package org.buildcli.commands.code;

import org.buildcli.actions.ai.AIChat;
import org.buildcli.actions.ai.AIServiceParams;
import org.buildcli.actions.ai.factories.GeneralAIServiceFactory;
import org.buildcli.actions.ai.params.JlamaAIServiceParams;
import org.buildcli.actions.ai.params.OllamaAIServiceParams;
import org.buildcli.constants.ConfigDefaultConstants;
import org.buildcli.domain.BuildCLICommand;
import org.buildcli.domain.configs.BuildCLIConfig;
import org.buildcli.handler.GlobalExceptionHandler;
import org.buildcli.utils.config.ConfigContextLoader;
import org.buildcli.utils.filesystem.FindFilesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.concurrent.CompletableFuture.supplyAsync;

@Command(name = "document", aliases = {"docs"}, description = "Generates documentation for the project code. Alias: 'docs'. This command scans the specified files and extracts structured documentation.", mixinStandardHelpOptions = true)
public class DocumentCommand implements BuildCLICommand {
  @ArgGroup
  private IAModel model;

  private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
  private final Logger logger = LoggerFactory.getLogger("CodeDocumentCommand");

  @Parameters(description = "Set of files or directories to comment sources")
  private List<File> files;

  @Option(names = {"--extensions", "--ext"}, description = "To filter files by", defaultValue = "java, kt, scala, groovy", paramLabel = "java, kt, scala, groovy")
  private String extensions;

  public static class IAModel {
    @Option(names = "--jlama")
    public boolean isJlama;
    @Option(names = "--ollama")
    public boolean isOllama;
  }

  private final BuildCLIConfig allConfigs = ConfigContextLoader.getAllConfigs();


  @Override
  public void run() {
    try {
      logger.warn("Use this command with careful, IA may be crazy!");

      if (files == null || files.isEmpty()) {
        logger.info("No files specified");
        return;
      }

      logger.info("Loading files with extensions: {}", Arrays.toString(getExtensions()));
      var targetFiles = files.parallelStream()
              .map(file -> FindFilesUtils.search(file, getExtensions()))
              .flatMap(List::stream)
              .toList();

      logger.info("Found {} files with extensions: {}.", targetFiles.size(), Arrays.toString(getExtensions()));

      var execsAsync = new CompletableFuture[targetFiles.size()];

      logger.info("Commenting files {}...", targetFiles.size());
      for (int i = 0; i < targetFiles.size(); i++) {
        execsAsync[i] = supplyAsync(createCodeDocumenter(targetFiles.get(i)), executorService)
                .thenAccept(saveSourceCodeDocumented(targetFiles.get(i)))
                .exceptionally(catchAnyError(targetFiles.get(i)));
      }

      CompletableFuture.allOf(execsAsync).join();
    } catch (Exception e) {
      GlobalExceptionHandler.handleException(e);
    }
  }

  private Supplier<String> createCodeDocumenter(File source) {
    try {
      logger.info("Reading source file: {}", source.getAbsolutePath());
      var sourceCode = Files.readString(source.toPath());
      logger.info("Source file read: {}", source.getAbsolutePath());

      var aiParams = createAIParamsFromConfigs();
      var iaService = new GeneralAIServiceFactory().create(aiParams);

      logger.info("Commenting with IA...");
      return () -> iaService.generate(new AIChat(sourceCode));

    } catch (IOException e) {
      return () -> {
        logger.warn("Could not read source file: {}", source.getAbsolutePath());
        throw new RuntimeException("Unable to read source file: " + source, e);
      };
    }
  }

  private AIServiceParams createAIParamsFromConfigs() {
    if (model == null) {
      var aiVendor = allConfigs.getProperty(ConfigDefaultConstants.AI_VENDOR).orElse("jlama");

      return switch (aiVendor.toLowerCase()) {
        case "ollama" -> {
          var url = allConfigs.getProperty(ConfigDefaultConstants.AI_URL).orElse(null);
          var model = allConfigs.getProperty(ConfigDefaultConstants.AI_MODEL).orElse(null);

          yield new OllamaAIServiceParams(url, model);
        }
        case "jlama" -> new JlamaAIServiceParams(allConfigs.getProperty(ConfigDefaultConstants.AI_MODEL).orElse(null));
        default -> throw new IllegalStateException("Unexpected AI Vendor: " + aiVendor);
      };
    } else {
      if (model.isJlama) {
        return new JlamaAIServiceParams(allConfigs.getProperty(ConfigDefaultConstants.AI_MODEL).orElse(null));
      } else if (model.isOllama) {
        var url = allConfigs.getProperty(ConfigDefaultConstants.AI_URL).orElse(null);
        var model = allConfigs.getProperty(ConfigDefaultConstants.AI_MODEL).orElse(null);

        return new OllamaAIServiceParams(url, model);
      } else {
        throw new IllegalStateException("AI Vendor is required");
      }
    }
  }

  private Consumer<String> saveSourceCodeDocumented(File file) {
    return sourceCode -> {
      try {
        Files.writeString(file.toPath(), sourceCode);
        logger.info("Source Code updated: {}", file.getAbsolutePath());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    };
  }

  private Function<Throwable, Void> catchAnyError(File file) {
    return throwable -> {
      var message = "Occurred an error when try comment code, file: %s".formatted(file);
      logger.error(message, throwable);

      return null;
    };
  }

  private String[] getExtensions() {
    if (extensions == null || extensions.isEmpty()) {
      return new String[]{"all"};
    }

    return Arrays.stream(extensions.split(",")).map(String::trim).map(".%s"::formatted).toArray(String[]::new);
  }

}
