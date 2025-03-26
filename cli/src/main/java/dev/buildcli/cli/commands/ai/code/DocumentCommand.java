package dev.buildcli.cli.commands.ai.code;

import dev.buildcli.cli.commands.ai.CodeCommand;
import dev.buildcli.core.actions.ai.AIChat;
import dev.buildcli.core.actions.ai.factories.GeneralAIServiceFactory;
import dev.buildcli.core.constants.AIConstants;
import dev.buildcli.core.domain.BuildCLICommand;
import dev.buildcli.core.domain.configs.BuildCLIConfig;
import dev.buildcli.core.utils.async.Async;
import dev.buildcli.core.utils.config.ConfigContextLoader;
import dev.buildcli.core.utils.filesystem.FindFilesUtils;
import dev.buildcli.core.utils.ai.CodeUtils;
import dev.buildcli.core.utils.ai.IAParamsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.concurrent.CompletableFuture.supplyAsync;

@Command(name = "document", aliases = {"docs"}, description = "Generates documentation for the project code. Alias: 'docs'. This command scans the specified files and extracts structured documentation.", mixinStandardHelpOptions = true)
public class DocumentCommand implements BuildCLICommand {
  private final Logger logger = LoggerFactory.getLogger(DocumentCommand.class);
  @ParentCommand
  private CodeCommand parent;

  @Parameters(description = "Set of files or directories to comment sources")
  private List<File> files;

  @Option(names = {"--extensions", "--ext"}, description = "To filter files by", defaultValue = "java, kt, scala, groovy", paramLabel = "java, kt, scala, groovy")
  private String extensions;

  @Option(names = {"--context"}, description = "Overwrite the default AI command")
  private String context;

  private final BuildCLIConfig allConfigs = ConfigContextLoader.getAllConfigs();


  @Override
  public void run() {
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

    var execsAsync = Async.group(targetFiles.size());;

    logger.info("Documenting files {}...", targetFiles.size());
    for (int i = 0; i < targetFiles.size(); i++) {
      execsAsync[i] = Async.run(createCodeDocumenter(targetFiles.get(i)))
          .then(CodeUtils::extractCode)
          .consumeAsync(saveSourceCodeDocumented(targetFiles.get(i)))
          .catchAny(catchAnyError(targetFiles.get(i)));
    }

    Async.awaitAll(execsAsync);
  }


  private Supplier<String> createCodeDocumenter(File source) {
    try {
      logger.info("Reading source file: {}", source.getAbsolutePath());
      var sourceCode = Files.readString(source.toPath());
      logger.info("Source file read: {}", source.getAbsolutePath());

      var aiParams = IAParamsUtils.createAIParams(parent.getModel(), parent.getVendor());;
      var iaService = new GeneralAIServiceFactory().create(aiParams);

      logger.info("Commenting with IA...");
      return () -> iaService.generate(new AIChat(context == null || context.isEmpty() ? AIConstants.DOCUMENT_CODE_PROMPT : context, sourceCode));

    } catch (IOException e) {
      return () -> {
        logger.warn("Could not read source file: {}", source.getAbsolutePath());
        throw new RuntimeException("Unable to read source file: " + source, e);
      };
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
