package com.github.argherna.ajpbin;

import static com.github.argherna.ajpbin.Xml.newNamespaceSAXParser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import javax.xml.parsers.SAXParserFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;

class LockHandlerTest {

  private static final String LS = System.getProperty("line.separator");

  private static final String TEMP_XML_FILE_NAME_PREFIX =
      LockHandlerTest.class.getName().replaceAll("\\.", "-");

  private static final String TEMP_XML_FILE_NAME_SUFFIX = ".xml";

  // private static File xml;

  private static StringBuilder xmlBuffer;

  private static String xmlFilename;

  @BeforeAll
  static void setupTestFiles() throws IOException {
    xmlBuffer = new StringBuilder();

    xmlBuffer.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>").append(LS)
        .append("<D:lockinfo xmlns:D=\"DAV:\">").append(LS)
        .append("  <D:lockscope><D:exclusive/></D:lockscope>").append(LS)
        .append("  <D:locktype><D:write/></D:locktype>\n").append(LS).append("  <D:owner>")
        .append(LS).append("    <D:href>http://www.ics.uci.edu/~ejw/contact.html</D:href>")
        .append(LS).append("  </D:owner>").append(LS).append("</D:lockinfo>");
  }

  @Test
  void testParseSimpleLockRequest() {
    var lockHandler = new Xml.LockHandler();
    try {
      var parser = newNamespaceSAXParser();
      parser.parse(new InputSource(new StringReader(xmlBuffer.toString())), lockHandler);
      assertEquals("exclusive", lockHandler.getLockScope());
      assertEquals("write", lockHandler.getLockType());
      assertEquals("http://www.ics.uci.edu/~ejw/contact.html", lockHandler.getOwnerHref());
    } catch (Exception e) {
      fail(e);
    }
  }
}
