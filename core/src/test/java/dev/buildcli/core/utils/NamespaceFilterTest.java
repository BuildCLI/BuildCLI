package dev.buildcli.core.utils;

import org.junit.jupiter.api.Test;

import javax.xml.stream.XMLStreamReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NamespaceFilterTest {
    @Test
    public void testGetNamespaceURI() {
        // Create a mock XMLStreamReader
        XMLStreamReader reader = mock(XMLStreamReader.class);
        when(reader.getNamespaceURI()).thenReturn("http://exemplo.com/ns");

        // Create a NamespaceFilter
        NamespaceFilter filter = new NamespaceFilter(reader);

        // The overridden method must return an empty string
        assertEquals("", filter.getNamespaceURI());
    }

    @Test
    public void testGetAttributeNamespace() {
        // Create a mock XMLStreamReader
        XMLStreamReader reader = mock(XMLStreamReader.class);
        when(reader.getAttributeNamespace(0)).thenReturn("http://exemplo.com/attr");

        // Create a NamespaceFilter
        NamespaceFilter filter = new NamespaceFilter(reader);

        // The overridden method must return an empty string
        assertEquals("", filter.getAttributeNamespace(0));
    }
}
