package com.github.argherna.ajpbin;

import static com.github.argherna.ajpbin.Constants.H_WEBDAV_BEGIN_IF_HEADER;
import static com.github.argherna.ajpbin.Constants.H_WEBDAV_DEPTH;
import static com.github.argherna.ajpbin.Constants.H_WEBDAV_DESTINATION;
import static com.github.argherna.ajpbin.Constants.H_WEBDAV_END_IF_HEADER;
import static com.github.argherna.ajpbin.Constants.H_WEBDAV_IF;
import static com.github.argherna.ajpbin.Constants.H_WEBDAV_INF;
import static com.github.argherna.ajpbin.Constants.H_WEBDAV_LOCKTOKEN;
import static com.github.argherna.ajpbin.Constants.H_WEBDAV_OVERWRITE;
import static com.github.argherna.ajpbin.Constants.MAX_MULTISTATUS;
import static com.github.argherna.ajpbin.Constants.SC_INSUFFICIENT_STORAGE;
import static com.github.argherna.ajpbin.Constants.SC_LOCKED;
import static com.github.argherna.ajpbin.Constants.SC_MULTI_STATUS;
import static com.github.argherna.ajpbin.Constants.STATUS_CODES_DESCRIPTIONS;
import static com.github.argherna.ajpbin.Constants.WEBDAV_OPAQUE_LOCK_TOKEN;
import static com.github.argherna.ajpbin.Xml.newNamespaceSAXParser;
import static java.util.logging.Level.WARNING;
import static javax.servlet.http.HttpServletResponse.SC_BAD_GATEWAY;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;
import static javax.servlet.http.HttpServletResponse.SC_CREATED;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_METHOD_NOT_ALLOWED;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_PRECONDITION_FAILED;
import static javax.servlet.http.HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE;

import java.io.IOException;
import java.security.SecureRandom;
import java.text.MessageFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

@WebServlet(name = "WebDavMethodsServlet", urlPatterns = { "/webdav/*" })
public class WebDavMethodsServlet extends HttpServlet {

  private static final Logger LOGGER = Logger.getLogger(WebDavMethodsServlet.class.getName());

  private static final String METHOD_DELETE = "DELETE";
  private static final String METHOD_HEAD = "HEAD";
  private static final String METHOD_GET = "GET";
  private static final String METHOD_OPTIONS = "OPTIONS";
  private static final String METHOD_POST = "POST";
  private static final String METHOD_PUT = "PUT";
  private static final String METHOD_TRACE = "TRACE";

  private static final String METHOD_COPY = "COPY";
  private static final String METHOD_LOCK = "LOCK";
  private static final String METHOD_MKCOL = "MKCOL";
  private static final String METHOD_MOVE = "MOVE";
  private static final String METHOD_PROPFIND = "PROPFIND";
  private static final String METHOD_PROPPATCH = "PROPPATCH";
  private static final String METHOD_UNLOCK = "UNLOCK";

  private static final String HEADER_IFMODSINCE = "If-Modified-Since";
  private static final String HEADER_LASTMOD = "Last-Modified";

  private static final String LSTRING_FILE = "com.github.argherna.ajpbin.LocalStrings";

  private static ResourceBundle lStrings = ResourceBundle.getBundle(LSTRING_FILE);

  private static final List<Integer> SC_COPY_ERROR_STATUS = List.of(SC_FORBIDDEN, SC_CONFLICT, SC_PRECONDITION_FAILED,
      SC_LOCKED, SC_BAD_GATEWAY, SC_INSUFFICIENT_STORAGE);

  private static final List<Integer> SC_MKCOL_ERROR_STATUS = List.of(SC_FORBIDDEN, SC_METHOD_NOT_ALLOWED, SC_CONFLICT,
      SC_UNSUPPORTED_MEDIA_TYPE, SC_INSUFFICIENT_STORAGE);

  private static final SecureRandom RANDOM = new SecureRandom();

  private static final String FILES = "files.properties";

  private final List<String> fileNames = new ArrayList<>();

  private final List<String> fileExtensions = new ArrayList<>();

  private final List<String> contentTypes = new ArrayList<>();

  @Override
  public void init() throws ServletException {
    try (var is = WebDavMethodsServlet.class.getResourceAsStream(FILES)) {
      var propsIn = new Properties();
      propsIn.load(is);
      fileNames.addAll(Arrays.asList(propsIn.getProperty("filenames").split(",")));
      fileExtensions.addAll(Arrays.asList(propsIn.getProperty("extensions").split(",")));
      contentTypes.addAll(Arrays.asList(propsIn.getProperty("contentTypes").split(",")));
    } catch (IOException e) {
      throw new ServletException(e);
    }
  }

  @Override
  protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String method = req.getMethod();

    if (method.equals(METHOD_GET)) {
      long lastModified = getLastModified(req);
      if (lastModified == -1) {
        // servlet doesn't support if-modified-since, no reason
        // to go through further expensive logic
        doGet(req, resp);
      } else {
        long ifModifiedSince = req.getDateHeader(HEADER_IFMODSINCE);
        if (ifModifiedSince < lastModified) {
          // If the servlet mod time is later, call doGet()
          // Round down to the nearest second for a proper compare
          // A ifModifiedSince of -1 will always be less
          maybeSetLastModified(resp, lastModified);
          doGet(req, resp);
        } else {
          resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
        }
      }

    } else if (method.equals(METHOD_HEAD)) {
      long lastModified = getLastModified(req);
      maybeSetLastModified(resp, lastModified);
      doHead(req, resp);

    } else if (method.equals(METHOD_POST)) {
      doPost(req, resp);

    } else if (method.equals(METHOD_PUT)) {
      doPut(req, resp);

    } else if (method.equals(METHOD_DELETE)) {
      doDelete(req, resp);

    } else if (method.equals(METHOD_OPTIONS)) {
      doOptions(req, resp);

    } else if (method.equals(METHOD_TRACE)) {
      doTrace(req, resp);

    } else if (method.equals(METHOD_COPY)) {
      doCopy(req, resp);

    } else if (method.equals(METHOD_LOCK)) {
      doLock(req, resp);

    } else if (method.equals(METHOD_MKCOL)) {
      doMkCol(req, resp);

    } else if (method.equals(METHOD_MOVE)) {
      doMove(req, resp);

    } else if (method.equals(METHOD_PROPFIND)) {
      doPropfind(req, resp);

    } else if (method.equals(METHOD_PROPPATCH)) {
      doProppatch(req, resp);

    } else if (method.equals(METHOD_UNLOCK)) {
      doUnlock(req, resp);

    } else {
      //
      // Note that this means NO servlet supports whatever
      // method was requested, anywhere on this server.
      //

      String errMsg = lStrings.getString("http.method_not_implemented");
      Object[] errArgs = new Object[1];
      errArgs[0] = method;
      errMsg = MessageFormat.format(errMsg, errArgs);

      resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, errMsg);
    }
  }

  /**
   * Return a response to the {@code COPY} method.
   * 
   * <p>
   * This method must have a resource specified in the path after the path for
   * this servlet or a {@value HttpServletResponse#SC_BAD_REQUEST} status is
   * returned.
   * 
   * <p>
   * This method must have a {@code Destination} header set in the request for
   * this servlet or a {@value HttpServletResponse#SC_BAD_REQUEST} status is
   * returned.
   * 
   * <p>
   * This method may have an {@code Overwrite} header set in the request for this
   * servlet. If not specified, a value of {@code F} is assumed.
   * 
   * <p>
   * You can specify query parameters on the Url to affect the functioning of this
   * method. This will let you test your service against different responses and
   * response codes. Acceptable parameters:
   * 
   * <dl>
   * <dt>{@code response_type}
   * <dd>Respond with the given type of message. Acceptable values are
   * {@code error} and {@code multistatus}.
   * <dt>{@code status_code}
   * <dd>Set to this status code if the {@code response_type} is {@code error}.
   * <dt>{@code resource_exists}
   * <dd>If {@code true}, treat the given resource as if it exists. If not set,
   * {@code false} is assumed.
   * </dl>
   * 
   * <p>
   * If a {@code response_type} of {@code multistatus} is requested, up to
   * {@value Constants#MAX_MULTISTATUS} multistatus responses can be returned in
   * the Xml payload. If there is an Xml document returned, the
   * {@code Content-Type} response header will be set to {@code text/xml}.
   * 
   * <p>
   * If no {@code response_type} is request, it is assumed that this is a copy of
   * a single resource and the outcome will be dependent on the expected behavior
   * between the value of the {@code Overwrite} header and the value of the
   * {@code resource_exists} parameter.
   * 
   * <p>
   * The HTTP statuses that this method can return are:
   * <ul>
   * <li>{@value HttpServletResponse#SC_CREATED} Created
   * <li>{@value HttpServletResponse#SC_NO_CONTENT} No Content
   * <li>{@value HttpServletResponse#SC_BAD_REQUEST} Bad Request (which is an
   * implementation detail of this method)
   * <li>{@value HttpServletResponse#SC_FORBIDDEN} Forbidden
   * <li>{@value HttpServletResponse#SC_CONFLICT} Conflict
   * <li>{@value HttpServletResponse#SC_PRECONDITION_FAILED} Precondition Failed
   * <li>{@value Constants#SC_LOCKED} Locked
   * <li>{@value HttpServletResponse#SC_BAD_GATEWAY} Bad Gateway
   * <li>{@value Constants#SC_INSUFFICIENT_STORAGE} Insufficient Storage
   * </ul>
   * 
   * @param request  the HttpServletRequest.
   * @param response the HttpServletResponse.
   * @throws IOException      if an IOException occurs.
   * @throws ServletException if a problem generating the Xml payload occurs.
   * @see http://www.webdav.org/specs/rfc2518.html#METHOD_COPY
   */
  protected void doCopy(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

    var path = request.getRequestURI().substring(request.getContextPath().length() + "/webdav".length());
    if (path.isEmpty()) {
      response.sendError(SC_BAD_REQUEST, STATUS_CODES_DESCRIPTIONS.get(SC_BAD_REQUEST));
      return;
    }

    var destination = request.getHeader(H_WEBDAV_DESTINATION);
    if (destination == null || destination.isEmpty()) {
      response.sendError(SC_BAD_REQUEST, lStrings.getString("webdav.copy_destination_not_set"));
      return;
    }

    if (destination.equals(path)) {
      response.sendError(SC_FORBIDDEN, lStrings.getString("webdav.copy_destination_eq_src"));
    }

    var overwrite = request.getHeader(H_WEBDAV_OVERWRITE);
    if (overwrite == null) {
      overwrite = "T";
    }

    var resourceExists = request.getParameter("resource_exists") != null
        ? Boolean.valueOf(request.getParameter("resource_exists"))
        : Boolean.FALSE;

    var responseType = request.getParameter("response_type") != null ? request.getParameter("response_type") : "";

    if (responseType.equalsIgnoreCase("error")) {

      var status = request.getParameter("status_code") != null ? Integer.valueOf(request.getParameter("status_code"))
          : SC_COPY_ERROR_STATUS.get(RANDOM.nextInt(SC_COPY_ERROR_STATUS.size()));
      response.sendError(status, STATUS_CODES_DESCRIPTIONS.get(status));
      return;

    } else if (responseType.equalsIgnoreCase("multistatus")) {

      // Create random error statuses; we don't care what REALLY happens, but we care
      // about getting
      // the multistatus Xml back.
      var maxStatuses = 0;
      do {
        // Make sure this method generates at least 1 status, but no more than the max
        // number.
        maxStatuses = RANDOM.nextInt(MAX_MULTISTATUS);
      } while (maxStatuses == 0);

      var statuses = new ArrayList<Map.Entry<Integer, String>>();
      for (int i = 0; i < maxStatuses; i++) {
        statuses.add(new AbstractMap.SimpleEntry<Integer, String>(
            SC_COPY_ERROR_STATUS.get(RANDOM.nextInt(SC_COPY_ERROR_STATUS.size())), destUrl(request, "R" + i)));
      }

      try {

        response.setStatus(SC_MULTI_STATUS);
        setResponseHeaders(response);
        Xml.MultistatusXmlStream.newInstance(statuses).write(response.getOutputStream());
        return;

      } catch (XMLStreamException | FactoryConfigurationError e) {

        LOGGER.log(WARNING, lStrings.getString("webdav.xml_output_doc_generate_fail"), e);
        // Definitely signifies a programming problem. Re-throw as a ServletException
        // and make an issue to fix.
        throw new ServletException(e);

      }

    } else {

      var statusCode = SC_NO_CONTENT;
      // Just copy a single resource obeying the Overwrite header if the resource
      // exists.
      if (resourceExists) {
        if (overwrite.equalsIgnoreCase("F")) {
          statusCode = SC_PRECONDITION_FAILED;
        }
      } else {
        // Resource does not exist; copy results in creation of new resource.
        statusCode = SC_CREATED;
      }
      response.setStatus(statusCode);
      setResponseHeaders(response);
    }
  }

  /**
   * Return a response to the {@code LOCK} method.
   * 
   * <p>
   * This method may have a {@code Depth} header set in the request for this
   * servlet. If set, it must not be {@code 1} or a
   * {@value HttpServletResponse#SC_BAD_REQUEST} status is returned.
   * 
   * <p>
   * This method may have an {@code If} header set in the request for this
   * servlet. When present, the header's value must be of the form
   * {@code (<opaquelocktoken:UUID>)} where {@code UUID} is a generated UUID. The
   * presence of this header will cause this method to not attempt to parse any
   * Xml included in the body since this means the request is to refresh a lock.
   * 
   * <p>
   * You can specify query parameters on the Url to affect the functioning of this
   * method. This will let you test your service against different responses and
   * response codes. Acceptable parameters:
   * 
   * <dl>
   * <dt>{@code response_type}
   * <dd>Respond with the given type of message. Acceptable value is
   * {@code error}.
   * <dt>{@code lock_tokens_match}
   * <dd>Either {@code true} or {@code false}; mimic behavior if an {@code If}
   * header with a lock token is set on the request.
   * </dl>
   * 
   * <p>
   * If no {@code response_type} is requested, it is assumed that a successful
   * processing will be executed.
   * 
   * <p>
   * The HTTP statuses that this method can return are:
   * <ul>
   * <li>{@value HttpServletResponse#SC_OK} OK
   * <li>{@value Constants#SC_MULTI_STATUS} Multistatus
   * <li>{@value HttpServletResponse#SC_BAD_REQUEST} Bad Request (which is an
   * implementation detail of this method)
   * <li>{@value HttpServletResponse#SC_PRECONDITION_FAILED} Precondition Failed
   * <li>{@value Constants#SC_LOCKED} Locked
   * </ul>
   * 
   * @param request  the HttpServletRequest.
   * @param response the HttpServletResponse.
   * @throws IOException      if an IOException occurs.
   * @throws ServletException
   * @see http://www.webdav.org/specs/rfc2518.html#METHOD_LOCK
   */
  protected void doLock(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

    var depth = request.getHeader(H_WEBDAV_DEPTH);
    if (depth != null && !depth.isEmpty()) {
      if (depth.equals("1")) {
        response.sendError(SC_BAD_REQUEST, lStrings.getString("webdav.lock_depth_is_1"));
        return;
      }
    } else {
      depth = H_WEBDAV_INF;
    }

    // This will be the resource to lock or extend the lock of.
    var resource = request.getRequestURI();

    var ifH = request.getHeader(H_WEBDAV_IF);
    var lockToken = ifH != null && validIfHeader(ifH) ? scrapeIfHeaderValue(ifH) : "";

    var responseType = request.getParameter("response_type") != null ? request.getParameter("response_type") : "";
    var lockTokensMatch = request.getParameter("lock_tokens_match") != null
        ? Boolean.valueOf(request.getParameter("lock_tokens_match"))
        : Boolean.TRUE;

    if (lockToken.length() > 0) {
      if (responseType.equalsIgnoreCase("error")) {
        response.sendError(SC_PRECONDITION_FAILED,
            MessageFormat.format(lStrings.getString("webdav.lock_dne"), resource));
        return;
      }

      if (lockTokensMatch) {
        setResponseHeaders(response);
        response.setStatus(SC_NO_CONTENT);
        return;
      } else {
        response.sendError(SC_PRECONDITION_FAILED,
        MessageFormat.format(lStrings.getString("webdav.lock_opaque_token_mismatch"), resource));
        return;
      }
      
    } else {
      if (responseType.equalsIgnoreCase("error")) {
        response.sendError(SC_LOCKED,
        MessageFormat.format(lStrings.getString("webdav.lock_resource_already_locked"), resource));
        return;
      }

      // Parse the Lock Xml in the request.
      var lockHandler = new Xml.LockHandler();
      try {
        newNamespaceSAXParser().parse(new InputSource(request.getInputStream()), lockHandler);
      } catch (ParserConfigurationException | SAXException e) {
        LOGGER.log(WARNING, lStrings.getString("webdav.xml_parse_lock_fail"), e);
        throw new ServletException(e);
      }

      // Generate Xml response from lock data.
      try {

        response.setStatus(SC_OK);
        setResponseHeaders(response);
        Xml.LockXmlStream.newInstance().lockType(lockHandler.getLockType())
            .lockscope(lockHandler.getLockScope().equals("exclusive") ? Xml.LockScope.EXCLUSIVE : Xml.LockScope.SHARED)
            .owner(lockHandler.getOwnerHref()).write(response.getOutputStream());
        return;

      } catch (XMLStreamException e) {
        LOGGER.log(WARNING, lStrings.getString("webdav.xml_output_doc_generate_fail"), e);
        // Definitely signifies a programming problem. Re-throw as a ServletException
        // and make an issue to fix.
        throw new ServletException(e);
      }
    }
  }

  /**
   * Return a response to the {@code MKCOL} method.
   * 
   * <p>
   * You can specify query parameters on the Url to affect the functioning of this
   * method. This will let you test your service against different responses and
   * response codes. Acceptable parameters:
   * 
   * <dl>
   * <dt>{@code response_type}
   * <dd>Respond with the given type of message. Acceptable value is {@code error}
   * <dt>{@code status_code}
   * <dd>Set to this status code if the {@code response_type} is {@code error}.
   * </dl>
   * 
   * <p>
   * If no {@code response_type} is set, it is assumed that this is a
   * {@code MKCOL} request that should succeed.
   * 
   * <p>
   * The HTTP statuses that this method can return are:
   * <ul>
   * <li>{@value HttpServletResponse#SC_CREATED} Created
   * <li>{@value HttpServletResponse#SC_FORBIDDEN} Forbidden
   * <li>{@value HttpServletResponse#SC_METHOD_NOT_ALLOWED} Method Not Allowed
   * <li>{@value HttpServletResponse#SC_CONFLICT} Conflict
   * <li>{@value HttpServletResponse#SC_UNSUPPORTED_MEDIA_TYPE} Unsupported Media
   * Type
   * <li>{@value Constants#SC_INSUFFICIENT_STORAGE} Insufficient Storage
   * </ul>
   * 
   * @param request  the HttpServletRequest.
   * @param response the HttpServletResponse.
   * @throws IOException      if an IOException occurs.
   * @throws ServletException if a problem generating the Xml payload occurs.
   * @see http://www.webdav.org/specs/rfc2518.html#METHOD_MKCOL
   */
  protected void doMkCol(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {

    var responseType = request.getParameter("response_type") != null ? request.getParameter("response_type") : "";
    var statusCode = Integer.MIN_VALUE;

    Throwable t = null;
    try {
      statusCode = request.getParameter("status_code") != null ? Integer.valueOf(request.getParameter("status_code"))
          : SC_MKCOL_ERROR_STATUS.get(RANDOM.nextInt(SC_MKCOL_ERROR_STATUS.size()));
    } catch (NumberFormatException e) {
      t = e;
    } finally {
      // Some wiseguy sent a non-number or a status code this method doesn't set, so
      // one will be chosen at random for them.
      if (t != null || !SC_MKCOL_ERROR_STATUS.contains(statusCode)) {
        statusCode = SC_MKCOL_ERROR_STATUS.get(RANDOM.nextInt(SC_MKCOL_ERROR_STATUS.size()));
      }
    }

    if (responseType.equalsIgnoreCase("error")) {
      response.sendError(statusCode, STATUS_CODES_DESCRIPTIONS.get(statusCode));
      return;
    }

    response.setStatus(SC_CREATED);
    setResponseHeaders(response);
  }

  /**
   * Return a response to the {@code MOVE} method.
   * 
   * <p>
   * This method must have a resource specified in the path after the path for
   * this servlet or a {@value HttpServletResponse#SC_BAD_REQUEST} status is
   * returned.
   * 
   * <p>
   * This method must have a {@code Destination} header set in the request for
   * this servlet or a {@value HttpServletResponse#SC_BAD_REQUEST} status is
   * returned.
   * 
   * <p>
   * This method may have an {@code Overwrite} header set in the request for this
   * servlet. If not specified, a value of {@code F} is assumed.
   * 
   * <p>
   * You can specify query parameters on the Url to affect the functioning of this
   * method. This will let you test your service against different responses and
   * response codes. Acceptable parameters:
   * 
   * <dl>
   * <dt>{@code response_type}
   * <dd>Respond with the given type of message. Acceptable values are
   * {@code error} and {@code multistatus}.
   * <dt>{@code status_code}
   * <dd>Set to this status code if the {@code response_type} is {@code error}.
   * <dt>{@code resource_exists}
   * <dd>If {@code true}, treat the given resource as if it exists. If not set,
   * {@code false} is assumed.
   * </dl>
   * 
   * <p>
   * If a {@code response_type} of {@code multistatus} is requested, up to
   * {@value Constants#MAX_MULTISTATUS} multistatus responses can be returned in
   * the Xml payload. If there is an Xml document returned, the
   * {@code Content-Type} response header will be set to {@code text/xml}.
   * 
   * <p>
   * If no {@code response_type} is request, it is assumed that this is a copy of
   * a single resource and the outcome will be dependent on the expected behavior
   * between the value of the {@code Overwrite} header and the value of the
   * {@code resource_exists} parameter.
   * 
   * <p>
   * The HTTP statuses that this method can return are:
   * <ul>
   * <li>{@value HttpServletResponse#SC_CREATED} Created
   * <li>{@value HttpServletResponse#SC_NO_CONTENT} No Content
   * <li>{@value HttpServletResponse#SC_BAD_REQUEST} Bad Request (which is an
   * implementation detail of this method)
   * <li>{@value HttpServletResponse#SC_FORBIDDEN} Forbidden
   * <li>{@value HttpServletResponse#SC_CONFLICT} Conflict
   * <li>{@value HttpServletResponse#SC_PRECONDITION_FAILED} Precondition Failed
   * <li>{@value Constants#SC_LOCKED} Locked
   * <li>{@value HttpServletResponse#SC_BAD_GATEWAY} Bad Gateway
   * <li>{@value Constants#SC_INSUFFICIENT_STORAGE} Insufficient Storage
   * </ul>
   * 
   * @param request  the HttpServletRequest.
   * @param response the HttpServletResponse.
   * @throws IOException      if an IOException occurs.
   * @throws ServletException if a problem generating the Xml payload occurs.
   * @see http://www.webdav.org/specs/rfc2518.html#METHOD_MOVE
   */
  protected void doMove(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
    doCopy(request, response);
  }

  /**
   * Return a response to the {@code PROPFIND} method.
   * 
   * <p>
   * only support {@code DAV:} properties
   * 
   * @param request
   * @param response
   * @throws IOException
   * @throws ServletException
   */
  protected void doPropfind(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    response.sendError(SC_METHOD_NOT_ALLOWED);
  }

  protected void doProppatch(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    response.sendError(SC_METHOD_NOT_ALLOWED);
  }

  /**
   * Return a response to the {@code COPY} method.
   * 
   * <p>
   * This method must have a {@value Constants#H_WEBDAV_LOCKTOKEN} header set in
   * the request for this servlet or a {@value HttpServletResponse#SC_BAD_REQUEST}
   * status is returned.
   * 
   * <p>
   * You can specify query parameters on the Url to affect the functioning of this
   * method. This will let you test your service against different responses and
   * response codes. Acceptable parameters:
   * 
   * <dl>
   * <dt>{@code response_type}
   * <dd>Respond with the given type of message. Acceptable value is
   * {@code error}.
   * </dl>
   * 
   * <p>
   * If no {@code response_type} is requested, it is assumed that a successful
   * unlock will be executed.
   * 
   * <p>
   * The HTTP statuses that this method can return are:
   * <ul>
   * <li>{@value HttpServletResponse#SC_NO_CONTENT} No Content
   * <li>{@value HttpServletResponse#SC_BAD_REQUEST} Bad Request (which is an
   * implementation detail of this method)
   * <li>{@value Constants#SC_LOCKED} Locked
   * </ul>
   * 
   * @param request  the HttpServletRequest.
   * @param response the HttpServletResponse.
   * @throws IOException      if an IOException occurs.
   * @throws ServletException if a problem generating the Xml payload occurs.
   * @see http://www.webdav.org/specs/rfc2518.html#METHOD_UNLOCK
   */
  protected void doUnlock(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    var lockToken = request.getHeader(H_WEBDAV_LOCKTOKEN);
    if (lockToken == null || lockToken.isEmpty()) {
      response.sendError(SC_BAD_REQUEST);
      return;
    }

    var responseType = request.getParameter("response_type") != null ? request.getParameter("response_type") : "";
    if (responseType.equalsIgnoreCase("error")) {
      response.setStatus(SC_LOCKED);
    } else {
      response.setStatus(SC_NO_CONTENT);
    }
    setResponseHeaders(response);
  }

  /*
   * Sets the Last-Modified entity header field, if it has not already been set
   * and if the value is meaningful. Called before doGet, to ensure that headers
   * are set before response data is written. A subclass might have set this
   * header already, so we check.
   */
  private void maybeSetLastModified(HttpServletResponse resp, long lastModified) {
    if (resp.containsHeader(HEADER_LASTMOD))
      return;
    if (lastModified >= 0)
      resp.setDateHeader(HEADER_LASTMOD, lastModified);
  }

  /**
   * Return a Url built from the request and the value of the {@code Destination}
   * header.
   * 
   * @param request the HttpServletRequest.
   * @return a Url.
   */
  private static String destUrl(HttpServletRequest request, String dest) {
    var url = new StringBuilder(request.getScheme()).append("://").append(request.getLocalName())
        .append(getPathUpToResourceName(request)).append(request.getHeader(H_WEBDAV_DESTINATION));
    if (!request.getHeader(H_WEBDAV_DESTINATION).endsWith("/")) {
      url.append("/");
    }
    return url.append(dest).toString();
  }

  private static String getPathUpToResourceName(HttpServletRequest request) {
    return request.getRequestURI().substring(0, request.getContextPath().length() + "/webdav".length());
  }

  private static boolean validIfHeader(String ifH) {
    return ifH.startsWith(H_WEBDAV_BEGIN_IF_HEADER) && ifH.contains(WEBDAV_OPAQUE_LOCK_TOKEN)
        && ifH.endsWith(H_WEBDAV_END_IF_HEADER);
  }

  private static String scrapeIfHeaderValue(String ifH) {
    return ifH.substring(H_WEBDAV_BEGIN_IF_HEADER.length(), ifH.indexOf(H_WEBDAV_END_IF_HEADER));
  }

  private void setResponseHeaders(HttpServletResponse response) {
    AjpbinHeaders.setHeaders(response);
    response.setContentType("text/xml");
  }
}
