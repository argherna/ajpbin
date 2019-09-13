package com.github.argherna.ajpbin;

import static com.github.argherna.ajpbin.Xml.WEBDAV_PROPERTIES;
import static com.github.argherna.ajpbin.Xml.newNamespaceSAXParser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;

import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;

class XmlTest {

  private static final String XML_WITH_NAMESPACE = "<?xml version=\"1.0\" encoding=\"utf-8\"?><propfind xmlns=\"DAV:\">"
      + "<allprop/></propfind>";
  private static final String XML_WITH_PREFIXED_NAMESPACE = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
      + "<D:propfind xmlns:D=\"DAV:\"><D:allprop/></D:propfind>";

  private static final String XML_WITH_NAMESPACE_SOME_PROPS = "<?xml version=\"1.0\" encoding=\"utf-8\"?><propfind xmlns=\"DAV:\">"
      + "<creationdate/><getcontenttype/><getcontentlength/><getlastmodified/><resourcetype/></propfind>";

  private static final String XML_WITH_PREFIXED_NAMESPACE_SOME_PROPS = "<?xml version=\"1.0\" encoding=\"utf-8\"?><D:propfind xmlns:D=\"DAV:\">"
      + "<D:creationdate/><D:getcontenttype/><D:getcontentlength/><D:getlastmodified/><D:resourcetype/></D:propfind>";

  private static final String XML_SMART_ALEC0 = "<?xml version=\"1.0\" encoding=\"utf-8\"?><propfind xmlns=\"DAV:\"/>";

  @Test
  void testLockXmlStream() {
    var sw = new StringWriter();
    try {
      Xml.LockXmlStream.newInstance().lockscope(Xml.LockScope.EXCLUSIVE).lockType("write").write(sw);
      System.err.println(sw.toString());
    } catch (XMLStreamException | FactoryConfigurationError e) {
      fail("" + e);
    }
  }

  @Test
  void testPropFindHandlerAllPropsWithNamespace() {
    var xml = new ByteArrayInputStream(XML_WITH_NAMESPACE.getBytes());
    try {
      var propFindHandler = new Xml.PropFindHandler();
      var parser = newNamespaceSAXParser();
      parser.parse(new InputSource(xml), propFindHandler);
      assertEquals(WEBDAV_PROPERTIES, propFindHandler.getProps());
    } catch (Exception e) {
      fail(e);
    }
  }

  @Test
  void testPropFindHandlerAllPropsWithPrefixedNamespace() {
    var xml = new ByteArrayInputStream(XML_WITH_PREFIXED_NAMESPACE.getBytes());
    try {
      var propFindHandler = new Xml.PropFindHandler();
      var parser = newNamespaceSAXParser();
      parser.parse(new InputSource(xml), propFindHandler);
      assertEquals(WEBDAV_PROPERTIES, propFindHandler.getProps());
    } catch (Exception e) {
      fail(e);
    }
  }

  @Test
  void testPropFindHandlerSomePropsWithNamespace() {
    var xml = new ByteArrayInputStream(XML_WITH_NAMESPACE_SOME_PROPS.getBytes());
    try {
      var propFindHandler = new Xml.PropFindHandler();
      var parser = newNamespaceSAXParser();
      parser.parse(new InputSource(xml), propFindHandler);
      var props = propFindHandler.getProps();
      assertFalse(props.isEmpty());
      for (String prop : props) {
        assertTrue(WEBDAV_PROPERTIES.contains(prop));
      }
    } catch (Exception e) {
      fail(e);
    }
  }

  @Test
  void testPropFindHandlerSomePropsWithPrefixedNamespace() {
    var xml = new ByteArrayInputStream(XML_WITH_PREFIXED_NAMESPACE_SOME_PROPS.getBytes());
    try {
      var propFindHandler = new Xml.PropFindHandler();
      var parser = newNamespaceSAXParser();
      parser.parse(new InputSource(xml), propFindHandler);
      var props = propFindHandler.getProps();
      assertFalse(props.isEmpty());
      for (String prop : props) {
        assertTrue(WEBDAV_PROPERTIES.contains(prop));
      }
    } catch (Exception e) {
      fail(e);
    }
  }

  @Test
  void testConfoundSmartAlec0() {
    var xml = new ByteArrayInputStream(XML_SMART_ALEC0.getBytes());
    try {
      var propFindHandler = new Xml.PropFindHandler();
      var parser = newNamespaceSAXParser();
      parser.parse(new InputSource(xml), propFindHandler);
      var props = propFindHandler.getProps();
      assertTrue(props.isEmpty());
    } catch (Exception e) {
      fail(e);
    }
  }
}