package dev.buildcli.cli.commands.project.update;

import java.util.logging.Level;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import dev.buildcli.cli.commands.project.BuildCommand;
import dev.buildcli.core.constants.MavenConstants;
import dev.buildcli.core.domain.BuildCLICommand;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "jdk", description = "Updates JDK version of the project.", mixinStandardHelpOptions = true)
public class JDKCommand implements BuildCLICommand {
    private static final Logger logger = LoggerFactory.getLogger(JDKCommand.class);
    @Option(names = {"--recomp", "-r"}, description = "Rebuilds the project with the new JDK version.", defaultValue = "false")
    private boolean recomp;
    @Option(names = {"--jdkver", "-jv"}, description = "The JDK version to update to.", defaultValue = "17")
    private String version;

    @Override
    public void run() {
        try {
            var docFactory = DocumentBuilderFactory.newInstance();
            docFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            var xmlDoc = docFactory.newDocumentBuilder().parse(MavenConstants.FILE);
            
            var node = xmlDoc.getElementsByTagName("maven.compiler.source").item(0);
            node.setTextContent(version);   
            
            var transformFactory = TransformerFactory.newInstance();
            transformFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            var transformer = transformFactory.newTransformer();

            try {
                transformer.transform(new DOMSource(xmlDoc), new StreamResult(MavenConstants.FILE));
                logger.info("JDK version updated in pom.xml");
            } catch (Exception e) {
                logger.error("Error updating JDK version in pom.xml", e);
            }
        } catch (Exception e) {
            logger.error("Error updating JDK version in pom.xml", e);
        }

        if (recomp) {
            logger.info("Rebuilding project with new java version");
            new BuildCommand().run();
        }
    }
}
