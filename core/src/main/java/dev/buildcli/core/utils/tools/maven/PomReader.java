package dev.buildcli.core.utils.tools.maven;

import jakarta.xml.bind.JAXBContext;
import dev.buildcli.core.constants.MavenConstants;
import dev.buildcli.core.exceptions.ExtractionRuntimeException;
import dev.buildcli.core.model.Dependency;
import dev.buildcli.core.model.Pom;
import dev.buildcli.core.utils.NamespaceFilter;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
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
import java.nio.file.Paths;
import java.util.Objects;
import java.util.stream.IntStream;

public class PomReader {
  public static Pom read(String fileName) {
    var pathFile = Paths.get(fileName);
    var pomFile = new File(pathFile.toFile().getAbsolutePath());

    try {
      var unmarshaller = JAXBContext.newInstance(Pom.class).createUnmarshaller();

      // Set up XML input with namespace filtering
      var xmlInputFactory = XMLInputFactory.newFactory();
      xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false); // prevent XXE attack
      var filter = new NamespaceFilter(xmlInputFactory.createXMLStreamReader(new StreamSource(pomFile)));

      return unmarshaller.unmarshal(filter, Pom.class).getValue();
    } catch (Exception e) {
      throw new ExtractionRuntimeException(e);
    }
  }

  public static String readAsString(String fileName) throws ParserConfigurationException, IOException, SAXException, TransformerException {
    var docFactory = DocumentBuilderFactory.newInstance();
    docFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true); // prevent XXE attack
    var xmlDoc = docFactory.newDocumentBuilder().parse(fileName);
    var nodes = xmlDoc.getElementsByTagName(Dependency.XML_WRAPPER_ELEMENT);

    var dependenciesNode = IntStream.range(0, nodes.getLength())
        .filter(i -> nodes.item(i).getParentNode().getNodeName().equals(Pom.XML_ELEMENT))
        .mapToObj(nodes::item)
        .findFirst()
        .orElse(null);

    var dependencyPatternNode = xmlDoc.createTextNode(MavenConstants.DEPENDENCIES_PATTERN);

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

    return outputString.toString();
  }

  public static String addOrUpdateDependency(String fileName, Dependency dependency)
      throws ParserConfigurationException, IOException, SAXException, TransformerException {
    Document document = getDocument(fileName);

    Element dependencies = getDependenciesElement(document);

    if (dependencies == null) {
      dependencies = getCreatedDependencies(document, document.getDocumentElement());
    }

    if (updateDependency(document, dependencies, dependency)) {
      return printDocument(document);
    }

    addComment(document, dependencies, MavenConstants.ADDED_COMMENT);
    addDependency(document, dependencies, dependency);

    return printDocument(document);
  }

  private static Document getDocument(String fileName)
      throws ParserConfigurationException, SAXException, IOException {
    var docFactory = DocumentBuilderFactory.newInstance();
    docFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true); // prevent XXE attack
    return docFactory.newDocumentBuilder().parse(fileName);
  }

  private static Element getDependenciesElement(Document document) {
    var nodes = document.getElementsByTagName(Dependency.XML_WRAPPER_ELEMENT);

    Node dependenciesNode = IntStream.range(0, nodes.getLength())
        .filter(i -> nodes.item(i).getParentNode().getNodeName().equals(Pom.XML_ELEMENT))
        .mapToObj(nodes::item)
        .findFirst()
        .orElse(null);

    return dependenciesNode == null ? null : (Element) dependenciesNode;
  }

  private static Element getCreatedDependencies(Document document, Element parent) {
    Element properties = (Element) document.getElementsByTagName("properties").item(0);

    Element dependencies = document.createElement("dependencies");
    parent.insertBefore(dependencies, properties.getNextSibling());
    parent.insertBefore(document.createTextNode("\n\n\t\t"), dependencies);
    parent.appendChild(document.createTextNode("\n"));
    return getDependenciesElement(document);
  }

  private static boolean updateDependency(Document document, Element dependencies, Dependency newDependency) {
    boolean isUpdated = false;

    NodeList nodes = dependencies.getElementsByTagName("dependency");
    for (int i = 0; i < nodes.getLength(); i++) {
      Element existentDependency = (Element) nodes.item(i);
      String groupId = getElementText(existentDependency, "groupId");
      String artifactId = getElementText(existentDependency, "artifactId");
      String version = getElementText(existentDependency, "version");

      if (!groupId.equals(newDependency.getGroupId())) {
        continue;
      }

      if (!artifactId.equals(newDependency.getArtifactId())) {
        continue;
      }

      if (version.isEmpty()) {
        Element versionElement = document.createElement("version");
        versionElement.setTextContent(newDependency.getVersion());
        existentDependency.appendChild(document.createTextNode("\t"));
        existentDependency.appendChild(versionElement);
        existentDependency.appendChild(document.createTextNode("\n\t\t"));
      } else {
        existentDependency.getElementsByTagName("version").item(0).setTextContent(newDependency.getVersion());
      }

      isUpdated = true;
      break;
    }

    return isUpdated;
  }

  private static String getElementText(Element parent, String tagName) {
    NodeList nodeList = parent.getElementsByTagName(tagName);
    if (nodeList.getLength() > 0) {
      return nodeList.item(0).getTextContent().trim();
    }
    return "";
  }

  private static String printDocument(Document document) throws TransformerException {
    var transformFactory = TransformerFactory.newInstance();
    transformFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true); // prevent XXE attack

    var transformer = transformFactory.newTransformer();
    var outputString = new StringWriter();
    transformer.transform(new DOMSource(document), new StreamResult(outputString));

    return outputString.toString();
  }

  private static void addComment(Document document, Element parent, String commentText) {
    parent.appendChild(document.createTextNode("\n"));
    Comment comment = document.createComment(commentText);
    parent.appendChild(document.createTextNode("\t\t\t"));
    parent.appendChild(comment);
  }

  private static void addDependency(Document document, Element parent, Dependency newDependency) {

    Element dependency = document.createElement("dependency");
    appendNewLineWith3Indentation(document, parent);

    Element group = document.createElement("groupId");
    appendNewLineWith4Indentation(document, dependency);
    group.appendChild(document.createTextNode(newDependency.getGroupId()));
    dependency.appendChild(group);

    Element artifact = document.createElement("artifactId");
    appendNewLineWith4Indentation(document, dependency);
    artifact.appendChild(document.createTextNode(newDependency.getArtifactId()));
    dependency.appendChild(artifact);

    if (newDependency.getVersion() != null) {
      Element ver = document.createElement("version");
      appendNewLineWith4Indentation(document, dependency);
      ver.appendChild(document.createTextNode(newDependency.getVersion()));
      dependency.appendChild(ver);
    }

    if (newDependency.getScope() != null) {
      Element sc = document.createElement("scope");
      appendNewLineWith4Indentation(document, dependency);
      sc.appendChild(document.createTextNode(newDependency.getScope()));
      dependency.appendChild(sc);
    }

    appendNewLineWith3Indentation(document, dependency);
    parent.appendChild(dependency);
    parent.appendChild(document.createTextNode("\n\t\t"));
  }

  public static void appendNewLineWith3Indentation(Document doc, Node parentNode) {
    parentNode.appendChild(doc.createTextNode("\n\t\t\t"));
  }
  public static void appendNewLineWith4Indentation(Document doc, Node parentNode) {
    parentNode.appendChild(doc.createTextNode("\n\t\t\t\t"));
  }
}
