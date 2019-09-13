package com.github.argherna.ajpbin;

import static com.github.argherna.ajpbin.Constants.PROTOCOL;
import static com.github.argherna.ajpbin.Constants.STATUS_CODES_DESCRIPTIONS;
import static com.github.argherna.ajpbin.Constants.WEBDAV_DEFAULT_LOCK_DURATION;
import static com.github.argherna.ajpbin.Constants.WEBDAV_DEFAULT_LOCK_OWNER;
import static com.github.argherna.ajpbin.Constants.WEBDAV_OPAQUE_LOCK_TOKEN;
import static com.github.argherna.ajpbin.Constants.XML_CHARSET_UTF_8;
import static com.github.argherna.ajpbin.Constants.XML_VERSION_1_0;
import static java.util.UUID.randomUUID;

import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Xml utility methods and factories.
 */
final class Xml {

  private static final Logger LOGGER = Logger.getLogger(Xml.class.getName());

  static final String NS_DAV = "DAV:";
  static final String NS_DAV_PREFIX = "D";

  static final String EL_ACTIVELOCK = "activelock";
  static final String EL_ALLPROP = "allprop";
  static final String EL_CREATIONDATE = "creationdate";
  static final String EL_DISPLAYNAME = "displayname";
  static final String EL_GETCONTENTLANGUAGE = "getcontentlanguage";
  static final String EL_GETCONTENTLENGTH = "getcontentlength";
  static final String EL_GETCONTENTTYPE = "getcontenttype";
  static final String EL_GETETAG = "getetag";
  static final String EL_GETLASTMODIFIED = "getlastmodified";
  static final String EL_HREF = "href";
  static final String EL_LOCKDISCOVERY = "lockdiscovery";
  static final String EL_LOCKSCOPE = "lockscope";
  static final String EL_LOCKTOKEN = "locktoken";
  static final String EL_LOCKTYPE = "locktype";
  static final String EL_MULTISTATUS = "multistatus";
  static final String EL_OWNER = "owner";
  static final String EL_PROP = "prop";
  static final String EL_PROPFIND = "propfind";
  static final String EL_PROPNAME = "propname";
  static final String EL_PROPSTAT = "propstat";
  static final String EL_RESOURCETYPE = "resourcetype";
  static final String EL_RESPONSE = "response";
  static final String EL_SOURCE = "source";
  static final String EL_STATUS = "status";
  static final String EL_SUPPORTEDLOCK = "supportedlock";
  static final String EL_TIMEOUT = "timeout";
  static final String EL_WRITE = "write";

  static final Set<String> WEBDAV_PROPERTIES = Set.of(EL_CREATIONDATE, EL_DISPLAYNAME, EL_GETCONTENTLANGUAGE,
      EL_GETCONTENTLENGTH, EL_GETCONTENTTYPE, EL_GETETAG, EL_GETLASTMODIFIED, EL_LOCKDISCOVERY, EL_RESOURCETYPE,
      EL_SOURCE, EL_SUPPORTEDLOCK);

  private Xml() {
    // Empty constructor prevents instantiation.
  }

  /**
   * Factory for {@link Document}s.
   * 
   * @param namespaceAware set to {@code True} to create Documents that are
   *                       namespace aware.
   * @return a new Document.
   * @throws ParserConfigurationException this should not be thrown with the
   *                                      default JAXP implementation.
   */
  static Document newDocument(boolean namespaceAware) throws ParserConfigurationException {
    var dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(namespaceAware);
    var bldr = dbf.newDocumentBuilder();
    return bldr.newDocument();
  }

  /**
   * Factory for creating namespace aware {@link Document}s.
   * 
   * @return a new namespace aware Document.
   * @throws ParserConfigurationException this should not be thrown with the
   *                                      default JAXP implementation.
   */
  static Document newNamespaceDocument() throws ParserConfigurationException {
    return newDocument(true);
  }

  /**
   * Factory for prefixed namespaced Document {@link Element}s.
   * 
   * <p>
   * For example, let's say you have a namespace
   * {@code https://example.com/xml/ns} for an element {@code myelement} in your
   * document and you want it prefixed with {@code "ns"}. Calling this factory
   * will give you an Element that will render as {@code &lt;ns:myelement/&gt;}.
   * 
   * @param d             the originating Document.
   * @param namespaceURI  the namespace URI for the Element.
   * @param qualifiedName the name of the Element.
   * @param prefix        the prefix to use.
   * @return a prefixed namespaced Element.
   */
  static Element newPrefixedNamespacedElement(Document d, String namespaceURI, String qualifiedName, String prefix) {
    var elt = d.createElementNS(namespaceURI, qualifiedName);
    elt.setPrefix(prefix);
    return elt;
  }

  /**
   * Renders the given {@link Document} as a String.
   * 
   * @param doc the Document to convert to a String.
   * @return a String representation of the Document.
   * @throws TransformerException this should not be thrown with the default JAXP
   *                              implementation.
   */
  static String toString(Document doc) throws TransformerException {
    var writer = new StringWriter();
    render(doc, writer);
    var xml = writer.toString();
    LOGGER.finer(() -> {
      return String.format("xml=%s", xml);
    });
    return xml;
  }

  /**
   * Writes the given {@link Document} to the given {@link Writer}.
   * 
   * @param doc the Document
   * @param w   the Writer.
   * @throws TransformerException this should not be thrown with the default JAXP
   *                              implementation.
   */
  static void render(Document doc, Writer w) throws TransformerException {
    var domSource = new DOMSource(doc);
    var result = new StreamResult(w);
    var tf = TransformerFactory.newInstance();
    var transformer = tf.newTransformer();
    transformer.transform(domSource, result);
  }

  /**
   * Factory for a namespace aware {@link SAXParser}.
   * 
   * @return a namespace aware SAXParser.
   * @throws ParserConfigurationException this should not be thrown with the
   *                                      default JAXP implementation.
   * @throws SAXException                 this should not be thrown with the
   *                                      default JAXP implementation.
   */
  static SAXParser newNamespaceSAXParser() throws ParserConfigurationException, SAXException {
    return newSAXParser(true);
  }

  /**
   * Factory for {@link SAXParser}s.
   * 
   * @param namespaceAware set to {@code True} to create SAXParsers that are
   *                       namespace aware.
   * @return a SAXParser.
   * @throws ParserConfigurationException this should not be thrown with the
   *                                      default JAXP implementation.
   * @throws SAXException                 this should not be thrown with the
   *                                      default JAXP implementation.
   */
  static SAXParser newSAXParser(boolean namespaceAware) throws ParserConfigurationException, SAXException {
    var parserFactory = SAXParserFactory.newInstance();
    parserFactory.setNamespaceAware(namespaceAware);
    return parserFactory.newSAXParser();
  }

  static abstract class NamespaceDefaultHandler extends DefaultHandler {

    Map<String, String> namespaces;

    String getPrefix(String uri) {
      return Objects.isNull(namespaces) ? "" : (namespaces.containsKey(uri) ? namespaces.get(uri) : "");
    }

    boolean elementNameIs(String elementName, String prefix, String localName, String qName) {
      return qName.equals(prefix + ":" + elementName) || localName.equals(elementName);
    }

  }

  /**
   * {@link ContentHandler} for parsing {@code propfind} Xml.
   */
  static final class PropFindHandler extends NamespaceDefaultHandler {

    private boolean inPropFind = false;

    private boolean allProps = false;

    private Collection<String> props;

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
      if (Objects.isNull(namespaces)) {
        namespaces = new HashMap<>();
      }
      namespaces.put(uri, prefix);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
      var prefix = getPrefix(uri);
      if (elementNameIs(EL_PROPFIND, prefix, localName, qName)) {
        inPropFind = true;
      } else if (inPropFind) {
        if (WEBDAV_PROPERTIES.contains(localName)) {
          if (Objects.isNull(props)) {
            props = new ArrayList<>();
          }
          props.add(localName);
        } else if (elementNameIs(EL_ALLPROP, prefix, localName, qName)) {
          allProps = true;
        }
      }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
      var prefix = getPrefix(uri);
      if (elementNameIs(EL_PROPFIND, prefix, localName, qName)) {
        inPropFind = false;
      }
    }

    Collection<String> getProps() {
      // Because props is lazily initialized, check to make sure it's not null. This
      // covers the case where some smart-alec would send an empty <propfind/> tag as
      // input. Return an empty collection if props wasn't initialized.
      return allProps ? WEBDAV_PROPERTIES : Objects.nonNull(props) ? props : Set.of();
    }
  }

  /**
   * {@link org.xml.sax.ContentHandler} for parsing {@code lock} Xml.
   */
  static final class LockHandler extends NamespaceDefaultHandler {

    private boolean inHref = false;
    private boolean inLockscope = false;
    private boolean inLocktype = false;
    private boolean inOwner = false;

    private String lockScope;
    private String lockType;
    private StringBuilder ownerHref;

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
      if (Objects.isNull(namespaces)) {
        namespaces = new HashMap<>();
      }
      namespaces.put(uri, prefix);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
      var prefix = getPrefix(uri);
      if (elementNameIs(EL_LOCKSCOPE, prefix, localName, qName)) {
        inLockscope = true;
      } else if (elementNameIs(EL_LOCKTYPE, prefix, localName, qName)) {
        inLocktype = true;
      } else if (inLockscope) {
        lockScope = localName;
      } else if (inLocktype) {
        lockType = localName;
      } else if (elementNameIs(EL_OWNER, prefix, localName, qName)) {
        inOwner = true;
      } else if (inOwner && elementNameIs(EL_HREF, prefix, localName, qName)) {
        inHref = true;
      }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
      var prefix = getPrefix(uri);
      if (elementNameIs(EL_LOCKSCOPE, prefix, localName, qName)) {
        inLockscope = false;
      } else if (elementNameIs(EL_LOCKTYPE, prefix, localName, qName)) {
        inLocktype = false;
      } else if (elementNameIs(EL_OWNER, prefix, localName, qName)) {
        inOwner = false;
      } else if (elementNameIs(EL_HREF, prefix, localName, qName)) {
        inHref = false;
      }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
      if (Objects.isNull(ownerHref)) {
        ownerHref = new StringBuilder();
      }
      if (inOwner && inHref) {
        ownerHref.append(ch, start, length);
      }
    }

    String getOwnerHref() {
      return ownerHref.toString();
    }

    String getLockScope() {
      return lockScope;
    }

    String getLockType() {
      return lockType;
    }
  }

  static final class MultistatusXmlStream {

    private final Collection<Map.Entry<Integer, String>> statusValues;

    static MultistatusXmlStream newInstance(Collection<Map.Entry<Integer, String>> statusValues) {
      return new MultistatusXmlStream(statusValues);
    }

    MultistatusXmlStream(Collection<Map.Entry<Integer, String>> statusValues) {
      this.statusValues = new ArrayList<>();
      this.statusValues.addAll(statusValues);
    }

    void write(OutputStream os) throws XMLStreamException, FactoryConfigurationError {
      doWrite(XMLOutputFactory.newFactory().createXMLStreamWriter(os));
    }

    void write(Writer w) throws XMLStreamException, FactoryConfigurationError {
      doWrite(XMLOutputFactory.newFactory().createXMLStreamWriter(w));
    }

    void doWrite(XMLStreamWriter xsw) throws XMLStreamException {
      xsw.writeStartDocument(XML_CHARSET_UTF_8, XML_VERSION_1_0);
      xsw.setPrefix(NS_DAV_PREFIX, NS_DAV);
      xsw.writeStartElement(NS_DAV_PREFIX, EL_MULTISTATUS, NS_DAV); // <multistatus>

      for (Map.Entry<Integer, String> statusValue : statusValues) {
        xsw.writeStartElement(NS_DAV_PREFIX, EL_RESPONSE, NS_DAV); // <response>

        xsw.writeStartElement(NS_DAV_PREFIX, EL_HREF, NS_DAV); // <href>
        xsw.writeCharacters(statusValue.getValue());
        xsw.writeEndElement(); // </href>

        xsw.writeStartElement(NS_DAV_PREFIX, EL_STATUS, NS_DAV); // <status>
        xsw.writeCharacters(multistatusLine(statusValue.getKey()));
        xsw.writeEndElement(); // </status>

        xsw.writeEndElement(); // </response>
      }

      xsw.writeEndElement(); // </multistatus>
      xsw.writeEndDocument();
      xsw.close();
    }

    private String multistatusLine(int sc) {
      return new StringBuilder(PROTOCOL).append(" ").append(sc).append(" ").append(STATUS_CODES_DESCRIPTIONS.get(sc))
          .toString();
    }
  }

  static final class LockXmlStream {

    private LockScope lockScope;

    private Duration timeout;

    private UUID opaqueLockToken;

    private String lockType;

    private String owner = WEBDAV_DEFAULT_LOCK_OWNER;

    static LockXmlStream newInstance() {
      return new LockXmlStream();
    }

    LockXmlStream lockscope(LockScope lockscope) {
      if (Objects.nonNull(lockscope)) {
        this.lockScope = lockscope;
      }
      return this;
    }

    LockXmlStream timeout(Duration timeout) {
      if (Objects.nonNull(timeout)) {
        this.timeout = timeout;
      }
      return this;
    }

    LockXmlStream opaqueLockToken(UUID opaqueLockToken) {
      if (Objects.nonNull(opaqueLockToken)) {
        this.opaqueLockToken = opaqueLockToken;
      }
      return this;
    }

    LockXmlStream lockType(String lockType) {
      if (Objects.nonNull(lockType) && !lockType.isEmpty()) {
        this.lockType = lockType;
      }
      return this;
    }

    LockXmlStream owner(String owner) {
      if (Objects.nonNull(owner) && !owner.isEmpty()) {
        this.owner = owner;
      }
      return this;
    }

    void write(Writer w) throws XMLStreamException, FactoryConfigurationError {
      doWrite(XMLOutputFactory.newFactory().createXMLStreamWriter(w));
    }

    void write(OutputStream os) throws XMLStreamException, FactoryConfigurationError {
      doWrite(XMLOutputFactory.newFactory().createXMLStreamWriter(os));
    }

    private void doWrite(XMLStreamWriter xsw) throws XMLStreamException, FactoryConfigurationError {
      xsw.writeStartDocument(XML_CHARSET_UTF_8, XML_VERSION_1_0);
      xsw.setPrefix(NS_DAV_PREFIX, NS_DAV);
      xsw.writeStartElement(NS_DAV_PREFIX, EL_PROP, NS_DAV); // <prop>
      xsw.writeNamespace(NS_DAV_PREFIX, NS_DAV);
      xsw.writeStartElement(NS_DAV_PREFIX, EL_LOCKDISCOVERY, NS_DAV); // <lockdiscovery>
      xsw.writeStartElement(NS_DAV_PREFIX, EL_ACTIVELOCK, NS_DAV); // <activelock>

      // Yoda condition -- keeps NPE from happening so ¯\_(ツ)_/¯
      if ("write".equals(lockType)) {
        xsw.writeStartElement(NS_DAV_PREFIX, EL_LOCKTYPE, NS_DAV); // <locktype>
        xsw.writeEmptyElement(NS_DAV_PREFIX, EL_WRITE, NS_DAV); // <write/>
        xsw.writeEndElement(); // </locktype>
      }

      xsw.writeStartElement(NS_DAV_PREFIX, EL_LOCKSCOPE, NS_DAV); // <lockscope>
      xsw.writeEmptyElement(NS_DAV_PREFIX, lockScope.toString(), NS_DAV); // <LOCKSCOPE_VALUE/>
      xsw.writeEndElement(); // </lockscope>

      xsw.writeStartElement(NS_DAV_PREFIX, EL_OWNER, NS_DAV); // <owner>
      xsw.writeStartElement(NS_DAV_PREFIX, EL_HREF, NS_DAV); // <href>
      xsw.writeCharacters(Objects.isNull(owner) ? WEBDAV_DEFAULT_LOCK_OWNER : owner);
      xsw.writeEndElement(); // </href>
      xsw.writeEndElement(); // </owner>

      xsw.writeStartElement(NS_DAV_PREFIX, EL_TIMEOUT, NS_DAV); // <timeout>
      xsw.writeCharacters(String.format("Seconds-%d",
          Objects.isNull(timeout) ? WEBDAV_DEFAULT_LOCK_DURATION.toSeconds() : timeout.toSeconds()));
      xsw.writeEndElement(); // </timeout>

      xsw.writeStartElement(NS_DAV_PREFIX, EL_LOCKTOKEN, NS_DAV); // <locktoken>
      xsw.writeStartElement(NS_DAV_PREFIX, EL_HREF, NS_DAV); // <href>
      xsw.writeCharacters(String.format("%s%s", WEBDAV_OPAQUE_LOCK_TOKEN,
          Objects.isNull(opaqueLockToken) ? randomUUID().toString() : opaqueLockToken.toString()));
      xsw.writeEndElement(); // </href>
      xsw.writeEndElement(); // </locktoken>

      xsw.writeEndElement(); // </activelock>
      xsw.writeEndElement(); // </lockdiscovery>
      xsw.writeEndElement(); // </prop>
      xsw.writeEndDocument();
      xsw.flush();
      xsw.close();
    }
  }

  static enum LockScope {
    EXCLUSIVE, SHARED;

    public String toString() {
      return this.name().toLowerCase();
    }
  }
}
