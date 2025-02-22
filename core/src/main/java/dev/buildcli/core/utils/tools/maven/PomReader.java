package dev.buildcli.core.utils.tools.maven;

import jakarta.xml.bind.JAXBContext;
import dev.buildcli.core.constants.MavenConstants;
import dev.buildcli.core.exceptions.ExtractionRuntimeException;
import dev.buildcli.core.model.Dependency;
import dev.buildcli.core.model.Pom;
import dev.buildcli.core.utils.NamespaceFilter;
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
}
