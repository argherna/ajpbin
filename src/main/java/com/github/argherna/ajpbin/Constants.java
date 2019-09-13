package com.github.argherna.ajpbin;

import static javax.servlet.http.HttpServletResponse.SC_ACCEPTED;
import static javax.servlet.http.HttpServletResponse.SC_BAD_GATEWAY;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;
import static javax.servlet.http.HttpServletResponse.SC_CONTINUE;
import static javax.servlet.http.HttpServletResponse.SC_CREATED;
import static javax.servlet.http.HttpServletResponse.SC_EXPECTATION_FAILED;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_GATEWAY_TIMEOUT;
import static javax.servlet.http.HttpServletResponse.SC_GONE;
import static javax.servlet.http.HttpServletResponse.SC_HTTP_VERSION_NOT_SUPPORTED;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_LENGTH_REQUIRED;
import static javax.servlet.http.HttpServletResponse.SC_METHOD_NOT_ALLOWED;
import static javax.servlet.http.HttpServletResponse.SC_MOVED_PERMANENTLY;
import static javax.servlet.http.HttpServletResponse.SC_MULTIPLE_CHOICES;
import static javax.servlet.http.HttpServletResponse.SC_NON_AUTHORITATIVE_INFORMATION;
import static javax.servlet.http.HttpServletResponse.SC_NOT_ACCEPTABLE;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_NOT_IMPLEMENTED;
import static javax.servlet.http.HttpServletResponse.SC_NOT_MODIFIED;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_PARTIAL_CONTENT;
import static javax.servlet.http.HttpServletResponse.SC_PAYMENT_REQUIRED;
import static javax.servlet.http.HttpServletResponse.SC_PRECONDITION_FAILED;
import static javax.servlet.http.HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE;
import static javax.servlet.http.HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE;
import static javax.servlet.http.HttpServletResponse.SC_REQUEST_TIMEOUT;
import static javax.servlet.http.HttpServletResponse.SC_REQUEST_URI_TOO_LONG;
import static javax.servlet.http.HttpServletResponse.SC_RESET_CONTENT;
import static javax.servlet.http.HttpServletResponse.SC_SEE_OTHER;
import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;
import static javax.servlet.http.HttpServletResponse.SC_SWITCHING_PROTOCOLS;
import static javax.servlet.http.HttpServletResponse.SC_TEMPORARY_REDIRECT;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static javax.servlet.http.HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE;
import static javax.servlet.http.HttpServletResponse.SC_USE_PROXY;

import java.time.Duration;
import java.util.AbstractMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

/**
 * Constant values used in this package.
 */
final class Constants {

  /** WebDAV server error status */
  static final int SC_INSUFFICIENT_STORAGE = 507;

  /** WebDAV client error status */
  static final int SC_LOCKED = 423;

  /** WebDAV success status */
  static final int SC_MULTI_STATUS = 207;

  /**
   * Private constructor to prevent instantiation.
   */
  private Constants() {
  }

  /** Name of the output Map attribute containing Json data processed at the end of the request. */
  static final String OUTPUT_MAP_ATTR_NAME = Constants.class.getPackage().getName() + ".OutputMap";

  /**
   * Name of the output Document attribute containing Xml data processed at the end of the request.
   */
  static final String OUTPUT_DOCUMENT_ATTR_NAME =
      Constants.class.getPackage().getName() + ".OutputDocument";

  static final String OUTPUT_WRITER_ATTR_NAME = Constants.class.getPackageName() + ".OutputWriter";

  static final String LOCKSCOPE_ATTR_NAME = Constants.class.getPackage().getName() + ".Lockscope";

  static final String LOCKTYPE_ATTR_NAME = Constants.class.getPackage().getName() + ".Locktype";

  static final String OPAQUE_LOCKTOKEN_ATTR_NAME = Constants.class.getPackageName() + ".OpaqueLockToken";

  static final String OWNER_ATTR_NAME = Constants.class.getPackageName() + ".Owner";
  
  static final String TIMEOUT_ATTR_NAME = Constants.class.getPackageName() + ".Timeout";

  static final int MAX_MULTISTATUS = 6;

  /** Content-Type form url encoded. */
  static final String CT_FORM_URLENCODED = "application/x-www-form-urlencoded";

  /** Content-Type application/json. */
  static final String CT_APPLICATION_JSON = "application/json";

  /** Content-Type text/xml. */
  static final String CT_TEXT_XML = "text/xml";

  static final String H_CONTENT_TYPE = "Content-Type";

  static final String H_CONTENT_LENGTH = "Content-Length";

  static final String H_WEBDAV_BEGIN_IF_HEADER = "(<";

  static final String H_WEBDAV_END_IF_HEADER = ">)";

  static final String H_WEBDAV_DEPTH = "Depth";

  static final String H_WEBDAV_DESTINATION = "Destination";

  static final String H_WEBDAV_HOST = "Host";

  static final String H_WEBDAV_IF = "If";

  static final String H_WEBDAV_INF = "Infinity";

  static final String H_WEBDAV_LOCKTOKEN = "Lock-Token";

  static final String H_WEBDAV_OVERWRITE = "Overwrite";

  static final String H_WEBDAV_TIMEOUT = "Timeout";

  static final String WEBDAV_DEFAULT_LOCK_OWNER = "owner@ajpbin.webdav";

  static final String WEBDAV_OPAQUE_LOCK_TOKEN = "opaquelocktoken:";

  static final Duration WEBDAV_DEFAULT_LOCK_DURATION = Duration.ofMinutes(2);

  static final String PROTOCOL = "HTTP/1.1";
  
  static final String XML_CHARSET_UTF_8 = "utf-8";

  static final String XML_VERSION_1_0 = "1.0";

  static Map<Integer, String> STATUS_CODES_DESCRIPTIONS = Map.ofEntries(

      // Informational responses
      new AbstractMap.SimpleImmutableEntry<>(SC_CONTINUE, "Continue"),
      new AbstractMap.SimpleImmutableEntry<>(SC_SWITCHING_PROTOCOLS, "Switching Protocols"),

      // Success
      new AbstractMap.SimpleImmutableEntry<>(SC_OK, "OK"),
      new AbstractMap.SimpleImmutableEntry<>(SC_CREATED, "Created"),
      new AbstractMap.SimpleImmutableEntry<>(SC_ACCEPTED, "Accepted"),
      new AbstractMap.SimpleImmutableEntry<>(SC_NON_AUTHORITATIVE_INFORMATION,
          "Non-Authoritative Information"),
      new AbstractMap.SimpleImmutableEntry<>(SC_NO_CONTENT, "No Content"),
      new AbstractMap.SimpleImmutableEntry<>(SC_RESET_CONTENT, "Reset Content"),
      new AbstractMap.SimpleImmutableEntry<>(SC_PARTIAL_CONTENT, "Partial Content"),
      // WebDAV success
      new AbstractMap.SimpleImmutableEntry<>(SC_MULTI_STATUS, "Multi-Status"),

      // Redirection
      new AbstractMap.SimpleImmutableEntry<>(SC_MULTIPLE_CHOICES, "Multiple Choices"),
      new AbstractMap.SimpleImmutableEntry<>(SC_MOVED_PERMANENTLY, "Moved Permanently"),
      new AbstractMap.SimpleImmutableEntry<>(SC_FOUND, "Found"),
      new AbstractMap.SimpleImmutableEntry<>(SC_SEE_OTHER, "See Other"),
      new AbstractMap.SimpleImmutableEntry<>(SC_NOT_MODIFIED, "Not Modified"),
      new AbstractMap.SimpleImmutableEntry<>(SC_USE_PROXY, "Use Proxy"),
      new AbstractMap.SimpleImmutableEntry<>(SC_TEMPORARY_REDIRECT, "Temporary Redirect"),

      // Client errors
      new AbstractMap.SimpleImmutableEntry<>(SC_BAD_REQUEST, "Bad Request"),
      new AbstractMap.SimpleImmutableEntry<>(SC_UNAUTHORIZED, "Unauthorized"),
      new AbstractMap.SimpleImmutableEntry<>(SC_PAYMENT_REQUIRED, "Payment Required"),
      new AbstractMap.SimpleImmutableEntry<>(SC_FORBIDDEN, "Forbidden"),
      new AbstractMap.SimpleImmutableEntry<>(SC_NOT_FOUND, "Not Found"),
      new AbstractMap.SimpleImmutableEntry<>(SC_METHOD_NOT_ALLOWED, "Method Not Allowed"),
      new AbstractMap.SimpleImmutableEntry<>(SC_NOT_ACCEPTABLE, "Not Acceptable"),
      new AbstractMap.SimpleImmutableEntry<>(HttpServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED,
          "Proxy Authentication Required"),
      new AbstractMap.SimpleImmutableEntry<>(SC_REQUEST_TIMEOUT, "Request Timeout"),
      new AbstractMap.SimpleImmutableEntry<>(SC_CONFLICT, "Conflict"),
      new AbstractMap.SimpleImmutableEntry<>(SC_GONE, "Gone"),
      new AbstractMap.SimpleImmutableEntry<>(SC_LENGTH_REQUIRED, "Length Required"),
      new AbstractMap.SimpleImmutableEntry<>(SC_PRECONDITION_FAILED, "Precondition Failed"),
      new AbstractMap.SimpleImmutableEntry<>(SC_REQUEST_ENTITY_TOO_LARGE,
          "Request Entity Too Large"),
      new AbstractMap.SimpleImmutableEntry<>(SC_REQUEST_URI_TOO_LONG, "Request URI Too Long"),
      new AbstractMap.SimpleImmutableEntry<>(SC_UNSUPPORTED_MEDIA_TYPE, "Unsupported Media Type"),
      new AbstractMap.SimpleImmutableEntry<>(SC_REQUESTED_RANGE_NOT_SATISFIABLE,
          "Requested Range Not Satisfiable"),
      new AbstractMap.SimpleImmutableEntry<>(SC_EXPECTATION_FAILED, "Expectation Failed"),
      // WebDAV client errors
      new AbstractMap.SimpleImmutableEntry<>(SC_LOCKED, "Locked"),

      // Server errors
      new AbstractMap.SimpleImmutableEntry<>(SC_INTERNAL_SERVER_ERROR, "Internal Server Error"),
      new AbstractMap.SimpleImmutableEntry<>(SC_NOT_IMPLEMENTED, "Not Implemented"),
      new AbstractMap.SimpleImmutableEntry<>(SC_BAD_GATEWAY, "Bad Gateway"),
      new AbstractMap.SimpleImmutableEntry<>(SC_SERVICE_UNAVAILABLE, "Service Unavailable"),
      new AbstractMap.SimpleImmutableEntry<>(SC_GATEWAY_TIMEOUT, "Gateway Timeout"),
      new AbstractMap.SimpleImmutableEntry<>(SC_HTTP_VERSION_NOT_SUPPORTED,
          "HTTP Version Not Supported"),
      // WebDAV server errors
      new AbstractMap.SimpleImmutableEntry<>(SC_INSUFFICIENT_STORAGE, "Insufficient Storage"));
}
