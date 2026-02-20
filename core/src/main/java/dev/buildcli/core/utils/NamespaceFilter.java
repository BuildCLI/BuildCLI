package dev.buildcli.core.utils;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.util.StreamReaderDelegate;

public class NamespaceFilter extends StreamReaderDelegate {

  public NamespaceFilter(XMLStreamReader reader) {
    super(reader);
  }

  @Override
  public String getAttributeNamespace(int index) {
    return "";
  }

  @Override
  public String getNamespaceURI() {
    return "";
  }
}
