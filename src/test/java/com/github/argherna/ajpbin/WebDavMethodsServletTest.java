package com.github.argherna.ajpbin;

import static com.github.argherna.ajpbin.Constants.CT_TEXT_XML;
import static com.github.argherna.ajpbin.Constants.H_CONTENT_LENGTH;
import static com.github.argherna.ajpbin.Constants.H_CONTENT_TYPE;
import static com.github.argherna.ajpbin.Constants.H_WEBDAV_BEGIN_IF_HEADER;
import static com.github.argherna.ajpbin.Constants.H_WEBDAV_DEPTH;
import static com.github.argherna.ajpbin.Constants.H_WEBDAV_END_IF_HEADER;
import static com.github.argherna.ajpbin.Constants.H_WEBDAV_IF;
import static com.github.argherna.ajpbin.Constants.H_WEBDAV_LOCKTOKEN;
import static com.github.argherna.ajpbin.Constants.H_WEBDAV_TIMEOUT;
import static com.github.argherna.ajpbin.Constants.OUTPUT_DOCUMENT_ATTR_NAME;
import static com.github.argherna.ajpbin.Constants.SC_INSUFFICIENT_STORAGE;
import static com.github.argherna.ajpbin.Constants.SC_LOCKED;
import static com.github.argherna.ajpbin.Constants.WEBDAV_OPAQUE_LOCK_TOKEN;
import static com.github.argherna.ajpbin.ServletApiProxies.createHttpServletRequestProxy;
import static com.github.argherna.ajpbin.ServletApiProxies.createHttpServletResponseProxy;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;
import static javax.servlet.http.HttpServletResponse.SC_CREATED;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_GONE;
import static javax.servlet.http.HttpServletResponse.SC_METHOD_NOT_ALLOWED;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_PRECONDITION_FAILED;
import static javax.servlet.http.HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

class WebDavMethodsServletTest {

  private static final String LS = System.getProperty("line.separator");

  private WebDavMethodsServlet servlet;

  @BeforeEach
  void setup() throws ServletException {
    servlet = new WebDavMethodsServlet();
    servlet.init();
  }

  @AfterEach
  void teardown() {
    servlet.destroy();
  }

  @Test
  void testLockingNonLockedResource() {
    var reqIh = SerlvetApiInvocationHandler.builder().contextPath("/ajpbin").method("LOCK")
        .requestURI("/webdav/to_lock.doc")
        .requestHeaders(Map.of(H_WEBDAV_IF,
            List.of(H_WEBDAV_BEGIN_IF_HEADER + WEBDAV_OPAQUE_LOCK_TOKEN
                + UUID.randomUUID().toString() + H_WEBDAV_END_IF_HEADER)))
        .parameters(Map.of("response_type", List.of("error"))).build();
    var respIh = SerlvetApiInvocationHandler.builder().build();

    var request = createHttpServletRequestProxy(reqIh);
    var response = createHttpServletResponseProxy(respIh);

    try {
      servlet.service(request, response);
    } catch (ServletException | IOException e) {
      fail("Caught exception!", e);
    }
    assertEquals(SC_PRECONDITION_FAILED, respIh.getStatusCode());
    assertTrue(respIh.getSendErrorCallCount() == 1);
  }

  @Test
  void testLockDepthOfOne() {
    var reqIh = SerlvetApiInvocationHandler.builder().contextPath("/ajpbin").method("LOCK")
        .requestURI("/webdav/to_lock.doc")
        .requestHeaders(Map.of(H_WEBDAV_DEPTH, List.of("1"), H_WEBDAV_IF,
            List.of(H_WEBDAV_BEGIN_IF_HEADER + WEBDAV_OPAQUE_LOCK_TOKEN
                + UUID.randomUUID().toString() + H_WEBDAV_END_IF_HEADER)))
        .build();
    var respIh = SerlvetApiInvocationHandler.builder().build();

    var request = createHttpServletRequestProxy(reqIh);
    var response = createHttpServletResponseProxy(respIh);

    try {
      servlet.service(request, response);
    } catch (ServletException | IOException e) {
      fail("Caught exception!", e);
    }
    assertEquals(SC_BAD_REQUEST, respIh.getStatusCode());
    assertTrue(respIh.getSendErrorCallCount() == 1);
  }

  @Test
  void testLockWithGoodXml() {
    var xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"utf-8\"?>").append(LS)
        .append("<D:lockinfo xmlns:D=\"DAV:\">").append(LS)
        .append("  <D:lockscope><D:exclusive/></D:lockscope>").append(LS)
        .append("  <D:locktype><D:write/></D:locktype>").append(LS).append("  <D:owner>").append(LS)
        .append("    <D:href>http://www.example.com/~argherna/contact.html</D:href>").append(LS)
        .append("  </D:owner>").append(LS).append("</D:lockinfo>").toString();

    var reqIh = SerlvetApiInvocationHandler.builder().contextPath("/ajpbin").method("LOCK")
        .requestURI("/webdav/to_lock.doc")
        .requestHeaders(Map.of(H_CONTENT_TYPE, List.of(CT_TEXT_XML), H_CONTENT_LENGTH,
            List.of(Integer.toString(xml.length())), H_WEBDAV_TIMEOUT,
            List.of("Infinite, Second-4100000000")))
        .requestBodyText(xml).build();
    var respIh = SerlvetApiInvocationHandler.builder().build();

    var request = createHttpServletRequestProxy(reqIh);
    var response = createHttpServletResponseProxy(respIh);

    try {
      servlet.service(request, response);
    } catch (ServletException | IOException e) {
      fail("Caught exception!", e);
    }
    assertEquals(SC_OK, respIh.getStatusCode());
  }

  @Test
  void testUnlockNoErrors() {
    var reqIh = SerlvetApiInvocationHandler.builder().contextPath("/ajpbin").method("UNLOCK")
        .requestURI("/webdav/to_lock.doc")
        .requestHeaders(Map.of(H_WEBDAV_LOCKTOKEN, List.of("(<opaquelocktoken:abcd1234>)")))
        .build();
    var respIh = SerlvetApiInvocationHandler.builder().build();

    var request = createHttpServletRequestProxy(reqIh);
    var response = createHttpServletResponseProxy(respIh);

    try {
      servlet.service(request, response);
    } catch (ServletException | IOException e) {
      fail("Caught exception!", e);
    }
    assertEquals(SC_NO_CONTENT, respIh.getStatusCode());
  }

  @Test
  void testUnlockWithErrors() {
    var reqIh = SerlvetApiInvocationHandler.builder().contextPath("/ajpbin").method("UNLOCK")
        .requestURI("/webdav/to_lock.doc")
        .requestHeaders(Map.of(H_WEBDAV_LOCKTOKEN, List.of("(<opaquelocktoken:abcd1234>)")))
        .parameters(Map.of("response_type", List.of("error"))).build();
    var respIh = SerlvetApiInvocationHandler.builder().build();

    var request = createHttpServletRequestProxy(reqIh);
    var response = createHttpServletResponseProxy(respIh);

    try {
      servlet.service(request, response);
    } catch (ServletException | IOException e) {
      fail("Caught exception!", e);
    }
    assertEquals(SC_LOCKED, respIh.getStatusCode());
  }

  @Test
  void testUnlockBadRequest() {
    var reqIh = SerlvetApiInvocationHandler.builder().contextPath("/ajpbin").method("UNLOCK")
        .requestURI("/webdav/to_lock.doc").parameters(Map.of("response_type", List.of("error")))
        .build();
    var respIh = SerlvetApiInvocationHandler.builder().build();

    var request = createHttpServletRequestProxy(reqIh);
    var response = createHttpServletResponseProxy(respIh);

    try {
      servlet.service(request, response);
    } catch (Exception e) {
      fail("Caught exception!", e);
    }
    assertEquals(SC_BAD_REQUEST, respIh.getStatusCode());
  }

  @Test
  void testMkColSuccess() {
    var reqIh = SerlvetApiInvocationHandler.builder().contextPath("/ajpbin").method("MKCOL")
        .requestURI("/webdav/new_col/").build();
    var respIh = SerlvetApiInvocationHandler.builder().build();

    var request = createHttpServletRequestProxy(reqIh);
    var response = createHttpServletResponseProxy(respIh);

    try {
      servlet.service(request, response);
    } catch (Exception e) {
      fail("Caught exception!", e);
    }
    assertEquals(SC_CREATED, respIh.getStatusCode());
  }

  @Test
  void testMkColErrorWithoutStatusCodeParameter() {
    final List<Integer> expectedStatusCodes = List.of(SC_FORBIDDEN, SC_METHOD_NOT_ALLOWED,
        SC_CONFLICT, SC_UNSUPPORTED_MEDIA_TYPE, SC_INSUFFICIENT_STORAGE);
    var reqIh = SerlvetApiInvocationHandler.builder().contextPath("/ajpbin").method("MKCOL")
        .requestURI("/webdav/new_col/").parameters(Map.of("response_type", List.of("error")))
        .build();
    var respIh = SerlvetApiInvocationHandler.builder().build();

    var request = createHttpServletRequestProxy(reqIh);
    var response = createHttpServletResponseProxy(respIh);

    try {
      servlet.service(request, response);
    } catch (Exception e) {
      fail("Caught exception!", e);
    }
    assertTrue(expectedStatusCodes.contains(respIh.getStatusCode()));
  }

  @Test
  void testMkColErrorWithStatusCodeParameter() {
    var statusCode = SC_FORBIDDEN;
    var reqIh = SerlvetApiInvocationHandler.builder().contextPath("/ajpbin").method("MKCOL")
        .requestURI("/webdav/new_col/").parameters(Map.of("response_type", List.of("error"),
            "status_code", List.of(Integer.toString(statusCode))))
        .build();
    var respIh = SerlvetApiInvocationHandler.builder().build();

    var request = createHttpServletRequestProxy(reqIh);
    var response = createHttpServletResponseProxy(respIh);

    try {
      servlet.service(request, response);
    } catch (Exception e) {
      fail("Caught exception!", e);
    }
    assertEquals(statusCode, respIh.getStatusCode());
  }

  @Test
  void testMkColErrorWithNonNumericStatusCodeParameter() {
    final List<Integer> expectedStatusCodes = List.of(SC_FORBIDDEN, SC_METHOD_NOT_ALLOWED,
        SC_CONFLICT, SC_UNSUPPORTED_MEDIA_TYPE, SC_INSUFFICIENT_STORAGE);
    var reqIh = SerlvetApiInvocationHandler.builder().contextPath("/ajpbin").method("MKCOL")
        .requestURI("/webdav/new_col/")
        .parameters(Map.of("response_type", List.of("error"), "status_code", List.of("p00p")))
        .build();
    var respIh = SerlvetApiInvocationHandler.builder().build();

    var request = createHttpServletRequestProxy(reqIh);
    var response = createHttpServletResponseProxy(respIh);

    try {
      servlet.service(request, response);
    } catch (Exception e) {
      fail("Caught exception!", e);
    }
    assertTrue(expectedStatusCodes.contains(respIh.getStatusCode()));
  }

  @Test
  void testMkColErrorWithUnExpectedStatusCodeParameter() {
    var unexpected = SC_GONE; // This is not supposed to be returned by doMkCol().
    final List<Integer> expectedStatusCodes = List.of(SC_FORBIDDEN, SC_METHOD_NOT_ALLOWED,
        SC_CONFLICT, SC_UNSUPPORTED_MEDIA_TYPE, SC_INSUFFICIENT_STORAGE);
    var reqIh = SerlvetApiInvocationHandler.builder().contextPath("/ajpbin").method("MKCOL")
        .requestURI("/webdav/new_col/").parameters(Map.of("response_type", List.of("error"),
            "status_code", List.of(Integer.toString(unexpected))))
        .build();
    var respIh = SerlvetApiInvocationHandler.builder().build();

    var request = createHttpServletRequestProxy(reqIh);
    var response = createHttpServletResponseProxy(respIh);

    try {
      servlet.service(request, response);
    } catch (Exception e) {
      fail("Caught exception!", e);
    }
    assertTrue(expectedStatusCodes.contains(respIh.getStatusCode()));
    assertNotEquals(unexpected, respIh.getStatusCode());
  }
}
