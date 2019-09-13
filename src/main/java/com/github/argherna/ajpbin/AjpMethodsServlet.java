package com.github.argherna.ajpbin;

import static com.github.argherna.ajpbin.Constants.CT_APPLICATION_JSON;
import static com.github.argherna.ajpbin.Constants.CT_FORM_URLENCODED;
import static com.github.argherna.ajpbin.Constants.OUTPUT_MAP_ATTR_NAME;
import static com.github.argherna.ajpbin.Json.marshal;
import static com.github.argherna.ajpbin.Requests.getArguments;
import static com.github.argherna.ajpbin.Requests.getAttributes;
import static com.github.argherna.ajpbin.Requests.getForm;
import static com.github.argherna.ajpbin.Requests.getHeaders;
import static com.github.argherna.ajpbin.Requests.getDataAsString;
import static com.github.argherna.ajpbin.Requests.getRequestUrl;
import static com.github.argherna.ajpbin.Requests.parameterStringToMap;
import static java.lang.String.format;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "AjpMethodsServlet", urlPatterns = {"/http"})
public class AjpMethodsServlet extends HttpServlet {

  private static final Logger LOGGER = Logger.getLogger(AjpMethodsServlet.class.getName());

  @Override
  protected void doDelete(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    request.setAttribute(OUTPUT_MAP_ATTR_NAME, createBodylessRequestOutput(request));
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    request.setAttribute(OUTPUT_MAP_ATTR_NAME, createBodylessRequestOutput(request));
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    doMethodWithRequestBody(request, response);
  }

  @Override
  protected void doPut(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    doMethodWithRequestBody(request, response);
  }

  /**
   * Handles requests that have a body (POST, PUT, etc.).
   * 
   * @param request  the servlet request
   * @param response the servlet response
   * @throws ServletException if a ServletException is thrown
   * @throws IOException      if an IOException is thrown
   */
  private void doMethodWithRequestBody(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    var headers = getHeaders(request);
    var url = getRequestUrl(request);
    var attributes = getAttributes(request);

    Map<String, Object> form = Map.of();
    Map<String, Object> args = Map.of();
    Map<String, Object> json = Map.of();
    var data = "";

    // Process the payload.
    var contentType = request.getHeader("Content-Type");
    if (contentType == null || contentType.isEmpty()) {
      response.setStatus(SC_BAD_REQUEST);
      return;
    }

    switch (contentType) {

      // Can't use Requests.getArguments here since BOTH query strings AND form parameters would be
      // part of it. Also, we want a separate "form" field in the response json. So get the form
      // data out "manually" and add it to the output map. It's possible for a POST to have a query
      // string so using Requests.parameterStringToMap will be used to process both.
      case CT_FORM_URLENCODED:
        form = getForm(request);
        break;

      case CT_APPLICATION_JSON:
        data = getDataAsString(request);
        json = marshal(data);
        break;

      default:
        break;
    }

    // Process query string if it's there.
    if (request.getQueryString() != null && !request.getQueryString().isEmpty()) {
      LOGGER.fine(() -> {
        return format("request.getQueryString()=%s", request.getQueryString());
      });
      args = parameterStringToMap(request.getQueryString());
    }

    request.setAttribute(OUTPUT_MAP_ATTR_NAME, Map.of("headers", headers, "url", url, "attributes",
        attributes, "form", form, "args", args, "json", json, "data", data));
  }

  /**
   * Creates and returns a Map whose keys are the components of the output and whose values are the
   * values of those components.
   * 
   * <p>
   * This method should be used when parsing headers, attributes, urls, and request parameters.
   * 
   * @param request
   * @return
   */
  private Map<String, Object> createBodylessRequestOutput(HttpServletRequest request) {
    return Map.of("args", getArguments(request), "headers", getHeaders(request), "attributes",
        getAttributes(request), "url", getRequestUrl(request));
  }
}
