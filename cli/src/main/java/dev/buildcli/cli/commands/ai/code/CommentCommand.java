package dev.buildcli.cli.commands.ai.code;

import dev.buildcli.core.actions.ai.AIChat;
import dev.buildcli.core.actions.ai.factories.GeneralAIServiceFactory;
import dev.buildcli.core.constants.AIConstants;
import dev.buildcli.core.domain.BuildCLICommand;
import dev.buildcli.core.utils.async.Async;
import dev.buildcli.core.utils.filesystem.FindFilesUtils;
import dev.buildcli.core.utils.ai.IAParamsUtils;
import dev.buildcli.core.utils.console.markdown.MarkdownInterpreter;
import dev.buildcli.core.log.SystemOutLogger;
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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static dev.buildcli.core.utils.BeautifyShell.blueFg;
import static dev.buildcli.core.utils.BeautifyShell.brightGreenFg;

@Command(name = "comment", aliases = {"c"}, description = "Comments out the selected code.",mixinStandardHelpOptions = true)
public class CommentCommand implements BuildCLICommand {
  private final Logger logger = LoggerFactory.getLogger("AICodeCommentCommand");

  @Parameters(description = "Set of files or directories to comment sources")
  private List<File> files;

  @Option(names = {"--extensions", "--ext"}, description = "To filter files by", defaultValue = "java, kt, scala, groovy", paramLabel = "java, kt, scala, groovy")
  private String extensions;

  @Option(names = {"--context"}, description = "Overwrite the default AI command")
  private String context;

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

    var execsAsync = Async.group(targetFiles.size());

    logger.info("Commenting files {}...", targetFiles.size());
    for (int i = 0; i < targetFiles.size(); i++) {
      execsAsync[i] = Async.run(createCodeCommenter(targetFiles.get(i)))
          .consumeAsync(printCommentedCode(targetFiles.get(i)))
          .catchAny(catchAnyError(targetFiles.get(i)));
    }

    Async.awaitAll(execsAsync);
  }

  private Supplier<String> createCodeCommenter(File source) {
    try {
      logger.info("Reading source file: {}", source.getAbsolutePath());
      var sourceCode = Files.readString(source.toPath());
      logger.info("Source file read: {}", source.getAbsolutePath());

      var aiParams = IAParamsUtils.createAIParams();
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

  private Consumer<String> printCommentedCode(File file) {
    return comment -> {
      SystemOutLogger.println(brightGreenFg("=").repeat(130));
      SystemOutLogger.println("Commented file: " + blueFg(file.getAbsolutePath()) + "\n");

      SystemOutLogger.println(new MarkdownInterpreter().interpret(comment));

      SystemOutLogger.println(brightGreenFg("=").repeat(130));
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
