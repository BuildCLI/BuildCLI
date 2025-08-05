package dev.buildcli.cli.commands.ai.code;

import dev.buildcli.cli.commands.ai.CodeCommand;
import dev.buildcli.core.actions.ai.AIChat;
import dev.buildcli.core.actions.ai.factories.GeneralAIServiceFactory;
import dev.buildcli.core.domain.BuildCLICommand;
import dev.buildcli.core.utils.LanguageDetector;
import dev.buildcli.core.utils.ai.CodeUtils;
import dev.buildcli.core.utils.ai.IAParamsUtils;
import dev.buildcli.core.utils.async.Async;
import dev.buildcli.core.utils.filesystem.FindFilesUtils;
import dev.buildcli.core.utils.console.markdown.MarkdownInterpreter;
import dev.buildcli.core.log.SystemOutLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static dev.buildcli.core.constants.AIConstants.GENERATE_TEST_PROMPT;
import static dev.buildcli.core.utils.BeautifyShell.blueFg;
import static dev.buildcli.core.utils.console.input.InteractiveInputUtils.confirm;

@Command(
    name = "test",
    aliases = {"t"},
    description = "Automatically generates unit tests for source code files using AI. "
        + "Scans specified files or directories, identifies source files based on provided extensions, "
        + "and utilizes an AI-powered service to generate corresponding test code. "
        + "Supports interactive confirmation or auto-acceptance of generated tests.",
    mixinStandardHelpOptions = true
)
public class TestCommand implements BuildCLICommand {
  private static final Logger logger = LoggerFactory.getLogger("AICodeTestCommand");

  @ParentCommand
  private CodeCommand parent;
  @Parameters(description = "Set of files or directories to comment sources")
  private List<File> files;
  @Option(names = {"--extensions", "--ext"}, description = "To filter files by", defaultValue = "java, kt, scala, groovy", paramLabel = "java, kt, scala, groovy")
  private String extensions;
  @Option(names = {"--context"}, description = "Overwrite the default AI command")
  private String context;
  @Option(names = "-y", description = "Accept all code", defaultValue = "false")
  private boolean acceptAllCode;

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

    var execsAsync = Async.group(targetFiles.size());

    logger.info("Commenting files {}...", targetFiles.size());
    for (int i = 0; i < targetFiles.size(); i++) {
      execsAsync[i] = Async.run(createCodeTestGenerator(targetFiles.get(i)))
          .then(printFormattedCode(targetFiles.get(i)))
          .then(CodeUtils::extractCode)
          .consumeAsync(writeTestCode(targetFiles.get(i)))
          .catchAny(catchAnyError(targetFiles.get(i)));
    }

    Async.awaitAll(execsAsync);
  }

  private Function<String, String> printFormattedCode(File file) {
    return s -> {
      SystemOutLogger.println("");
      SystemOutLogger.println("File: " + blueFg(file.toString()));
      SystemOutLogger.println(new MarkdownInterpreter().interpret(s));
      return s;
    };
  }

  private Consumer<String> writeTestCode(File source) {
    return sourceCode -> {
      try {

        if (acceptAllCode || confirm("Do you agree with this code? %s".formatted(source.getName()))) {
          var file = source.getParentFile() == null ? source : source.getParentFile();
          var parts = source.getName().split("\\.");
          String simpleName;

          if (parts.length > 1) {
            var builder = new StringBuilder(parts[0] + "Test");

            for (int i = 1; i < parts.length; i++) {
              builder.append(".").append(parts[i]);
            }
            simpleName = builder.toString();
          } else {
            simpleName = source.getName();
          }

          var absolutePathValue = file.getName().equals(source.getName()) ? file.getAbsolutePath().replace(file.getName(), "") : file.getAbsolutePath();
          absolutePathValue = new File(absolutePathValue, simpleName).getAbsolutePath();

          var testPath = Paths.get(absolutePathValue.replaceFirst("main", "test"));
          Files.writeString(testPath, sourceCode);
        }

      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    };
  }

  private Supplier<String> createCodeTestGenerator(File source) {
    try {
      logger.info("Reading source file: {}", source.getAbsolutePath());
      var sourceCode = Files.readString(source.toPath());
      logger.info("Source file read: {}", source.getAbsolutePath());

      var aiParams = IAParamsUtils.createAIParams(parent.getModel(), parent.getVendor());
      var iaService = new GeneralAIServiceFactory().create(aiParams);

      var lang = LanguageDetector.detectLanguage(source.getName());

      logger.info("Generating test with IA...");
      return () -> iaService.generate(new AIChat(context == null || context.isEmpty() ? String.format(GENERATE_TEST_PROMPT, lang) : context, sourceCode));

    } catch (IOException e) {
      return () -> {
        logger.warn("Could not read source file: {}", source.getAbsolutePath());
        throw new RuntimeException("Unable to read source file: " + source, e);
      };
    }
  }

  private String[] getExtensions() {
    if (extensions == null || extensions.isEmpty()) {
      return new String[]{"all"};
    }

    return Arrays.stream(extensions.split(",")).map(String::trim).map(".%s"::formatted).toArray(String[]::new);
  }

  private Function<Throwable, Void> catchAnyError(File file) {
    return throwable -> {
      var message = "Occurred an error when try generating test code, file: %s".formatted(file);
      logger.error(message, throwable);

      return null;
    };
  }
}
