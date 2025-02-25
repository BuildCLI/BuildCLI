package dev.buildcli.core.utils;

import dev.buildcli.core.exceptions.ExtractionRuntimeException;
import dev.buildcli.core.log.SystemOutLogger;
import dev.buildcli.core.model.Dependency;
import dev.buildcli.core.model.Pom;
import io.github.dumijdev.dpxml.model.Node;
import io.github.dumijdev.dpxml.model.XMLNode;
import jakarta.xml.bind.JAXBContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class PomUtils {

  private static final Logger logger = LoggerFactory.getLogger(PomUtils.class);

  private static final String FILE = "pom.xml";
  private static final String DEPENDENCIES_PATTERN = "##dependencies##";
  private static String pomData;
  private static Pom pom = new Pom();

  private PomUtils() {
  }

  public static Pom addDependencyToPom(String pomPath, String[] dependencies) {
    extractPomFile(pomPath);
    Stream.of(dependencies).forEach(pom::addDependency);
    return pom;
  }

  public static void addDependencyToPom(String[] dependencies) {
    extractPomFile();
    Stream.of(dependencies).forEach(pom::addDependency);
    applyChangesToPom("Dependency added to pom.xml.", "Error adding dependency to pom.xml");
  }

  public static Pom rmDependencyToPom(String pomPath, String[] dependencies) {
    extractPomFile(pomPath);
    Stream.of(dependencies).forEach(pom::rmDependency);
    return pom;
  }

  public static void rmDependencyToPom(String[] dependencies) {
    extractPomFile();
    Stream.of(dependencies).forEach(pom::rmDependency);
    applyChangesToPom("Dependency removed from pom.xml.", "Error removing dependency from pom.xml");
  }

  private static void applyChangesToPom(String successMessage, String failureMessage) {

    try {
      String pomContent = pomData.replace(DEPENDENCIES_PATTERN, pom.getDependencyFormatted());
      Files.write(Paths.get(FILE), pomContent.getBytes());
      SystemOutLogger.log(successMessage);
    } catch (IOException e) {
      logger.error(failureMessage, e);
    }
  }

  public static void extractPomFile() {
    extractPomFile(FILE);
  }

  public static Pom extractPomFile(String pomPath) {

    var pathFile = Paths.get(pomPath);
    var pomFile = new File(pathFile.toFile().getAbsolutePath());

    try {
      var unmarshaller = JAXBContext.newInstance(Pom.class).createUnmarshaller();

      // Set up XML input with namespace filtering
      var xmlInputFactory = XMLInputFactory.newFactory();
      xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false); // prevent XXE attack
      var filter = new NamespaceFilter(xmlInputFactory.createXMLStreamReader(new StreamSource(pomFile)));

      pom = unmarshaller.unmarshal(filter, Pom.class).getValue();

      loadPomData(pomFile);

      return pom;
    } catch (Exception e) {
      throw new ExtractionRuntimeException(e);
    }
  }

  private static void loadPomData(File pomFile) throws
      ParserConfigurationException, SAXException, IOException, TransformerException {

    var docFactory = DocumentBuilderFactory.newInstance();
    docFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true); // prevent XXE attack
    var xmlDoc = docFactory.newDocumentBuilder().parse(pomFile);
    var nodes = xmlDoc.getElementsByTagName(Dependency.XML_WRAPPER_ELEMENT);

    var dependenciesNode = IntStream.range(0, nodes.getLength())
        .filter(i -> nodes.item(i).getParentNode().getNodeName().equals(Pom.XML_ELEMENT))
        .mapToObj(nodes::item)
        .findFirst()
        .orElse(null);

    var dependencyPatternNode = xmlDoc.createTextNode(DEPENDENCIES_PATTERN);

    if (Objects.isNull(dependenciesNode)) {
      xmlDoc.getElementsByTagName(Pom.XML_ELEMENT).item(0).appendChild(dependencyPatternNode);
    } else {
      dependenciesNode.getParentNode().replaceChild(dependencyPatternNode, dependenciesNode);
    }

    var transformFactory = TransformerFactory.newInstance();
    transformFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true); // prevent XXE attack

    var transformer = transformFactory.newTransformer();
    var outputString = new StringWriter();
    transformer.transform(new DOMSource(xmlDoc), new StreamResult(outputString));

    pomData = outputString.toString();
  }


  public static Node parseDependency(String line) {
    if (Objects.isNull(line)) {
      throw new ExtractionRuntimeException("dependency cannot be null");
    }

    Node dep;

    var parts = line.trim().split(":", 4);

    if (parts.length == 2) {
      var groupId = parts[0];
      var artifactId = parts[1];

      dep = createDependencyNode(groupId, artifactId, null, null);
    } else if (parts.length == 3) {
      var groupId = parts[0];
      var artifactId = parts[1];
      var version = parts[2];

      dep = createDependencyNode(groupId, artifactId, version, null);
    } else if (parts.length == 4) {
      var groupId = parts[0];
      var artifactId = parts[1];
      var version = parts[2];
      var scope = parts[3];

      dep = createDependencyNode(groupId, artifactId, version, scope);

    } else {
      logger.warn("Invalid dependency format: {}", line);
      throw new ExtractionRuntimeException("Invalid dependency format: " + line);
    }

    return dep;
  }

  private static Node createDependencyNode(String groupId, String artifactId, String version, String scope) {
    var dependencyNode = new XMLNode("dependency");

    if (Objects.nonNull(groupId)) {
      var node = new XMLNode("groupId");
      node.setContent(groupId);
      dependencyNode.addChild("groupId", node);
    }

    if (Objects.nonNull(artifactId)) {
      var node = new XMLNode("artifactId");
      node.setContent(artifactId);
      dependencyNode.addChild("artifactId", node);
    }

    if (Objects.nonNull(version)) {
      var node = new XMLNode("version");
      node.setContent(version);
      dependencyNode.addChild("version", node);
    }

    if (Objects.nonNull(scope)) {
      var node = new XMLNode("scope");
      node.setContent(scope);
      dependencyNode.addChild("scope", node);
    }

    return dependencyNode;
  }



}
