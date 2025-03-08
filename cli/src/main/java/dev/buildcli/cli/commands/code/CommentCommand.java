package dev.buildcli.cli.commands.code;

import dev.buildcli.core.actions.ai.AIChat;
import dev.buildcli.core.actions.ai.AIServiceParams;
import dev.buildcli.core.actions.ai.factories.GeneralAIServiceFactory;
import dev.buildcli.core.actions.ai.params.JlamaAIServiceParams;
import dev.buildcli.core.actions.ai.params.OllamaAIServiceParams;
import dev.buildcli.core.constants.AIConstants;
import dev.buildcli.core.constants.ConfigDefaultConstants;
import dev.buildcli.core.domain.BuildCLICommand;
import dev.buildcli.core.domain.configs.BuildCLIConfig;
import dev.buildcli.core.utils.config.ConfigContextLoader;
import dev.buildcli.core.utils.filesystem.FindFilesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@Command(name = "comment", aliases = {"c"}, description = "Comments out the selected code.",mixinStandardHelpOptions = true)
public class CommentCommand implements BuildCLICommand {
  private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
  private final Logger logger = LoggerFactory.getLogger(CommentCommand.class);

  @Parameters(description = "Set of files or directories to comment sources")
  private List<File> files;

  @Option(names = {"--extensions", "--ext"}, description = "To filter files by", defaultValue = "java, kt, scala, groovy", paramLabel = "java, kt, scala, groovy")
  private String extensions;

  @Option(names = {"--context"}, description = "Overwrite the default AI command")
  private String context;

  private final BuildCLIConfig allConfigs = ConfigContextLoader.getAllConfigs();


  @Override
  public void run() {
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
      execsAsync[i] = supplyAsync(createCodeCommenter(targetFiles.get(i)), executorService)
          .thenAccept(printCommentedCode(targetFiles.get(i)))
          .exceptionally(catchAnyError(targetFiles.get(i)));
    }

    CompletableFuture.allOf(execsAsync).join();
  }

  private Supplier<String> createCodeCommenter(File source) {
    try {
      logger.info("Reading source file: {}", source.getAbsolutePath());
      var sourceCode = Files.readString(source.toPath());
      logger.info("Source file read: {}", source.getAbsolutePath());

      var aiParams = createAIParamsFromConfigs();
      var iaService = new GeneralAIServiceFactory().create(aiParams);

      logger.info("Commenting with IA...");
      return () -> iaService.generate(new AIChat(context == null || context.isEmpty() ? AIConstants.COMMENT_CODE_PROMPT : context, sourceCode));

    } catch (IOException e) {
      return () -> {
        logger.warn("Could not read source file: {}", source.getAbsolutePath());
        throw new RuntimeException("Unable to read source file: " + source, e);
      };
    }
  }

  private AIServiceParams createAIParamsFromConfigs() {
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
  }

  private Consumer<String> printCommentedCode(File file) {
    return comment -> {
      System.out.println("=".repeat(130));
      System.out.printf("Commented file: %s%n%n", file.getAbsolutePath());

      System.out.println(comment);

      System.out.println("=".repeat(130));
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
