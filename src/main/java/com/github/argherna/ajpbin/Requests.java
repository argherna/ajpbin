package com.github.argherna.ajpbin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;

/**
 * Utilities for processing request information.
 */
final class Requests {

  private static final Logger LOGGER = Logger.getLogger(Requests.class.getName());

  /**
   * Private constructor to prevent instantiation.
   */
  private Requests() {
  }

  /**
   * Converts a query parameter string or form data string into a {@link Map} and returns it.
   * 
   * @param params query parameter or form data string.
   * @return Map whose keys are the parameter name and whose values are the parameter value(s).
   */
  static final Map<String, Object> parameterStringToMap(String params) {
    var parameters = new HashMap<String, Object>();
    if (params.length() > 0) {
      var namesAndValues = params.split("&");
      for (String param : namesAndValues) {
        var nameValue = param.split("=");
        if (parameters.containsKey(nameValue[0])) {
          var value = parameters.get(nameValue[0]);
          if (value instanceof List) {
            @SuppressWarnings("unchecked")
            var valList = (List<Object>) value;
            valList.add(nameValue[1]);
          } else {
            var valList = new ArrayList<Object>();
            valList.add(value);
            valList.add(nameValue[1]);
            parameters.replace(nameValue[0], valList);
          }
        } else {
          parameters.put(nameValue[0], nameValue[1]);
        }
      }
    }
    LOGGER.finer(() -> {
      return String.format("parameters=%s", parameters.toString());
    });
    return parameters;
  }

  /**
   * Returns a Map containing arguments whose key is the argument name and whose value(s) is/are the
   * argument value(s).
   * 
   * <p>
   * Use this method to extract query parameters and form parameters from the request.
   * 
   * @param request the {@link HttpServletRequest}.
   * @return Map of the arguments.
   */
  static final Map<String, Object> getArguments(HttpServletRequest request) {
    var arguments = new HashMap<String, Object>();
    var argumentNames = request.getParameterNames();

    while (argumentNames.hasMoreElements()) {
      var name = argumentNames.nextElement();
      var vals = request.getParameterValues(name);
      if (vals.length > 1) {
        var valsList = new ArrayList<String>();
        for (String val : vals) {
          valsList.add(val);
        }
        arguments.put(name, valsList);
      } else {
        arguments.put(name, vals[0]);
      }
    }
    LOGGER.finer(() -> {
      return String.format("arguments=%s", arguments.toString());
    });
    return arguments;
  }

  /**
   * Returns a Map containing only the form data posted.
   * 
   * @param request the HttpServletRequest.
   * @return Map containing the form data.
   */
  static Map<String, Object> getForm(HttpServletRequest request) {
    final var queryString = request.getQueryString() == null ? "" : request.getQueryString();

    return getArguments(request).entrySet().stream().filter(e -> !queryString.contains(e.getKey()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  /**
   * Returns a String of the contents of the payload.
   * 
   * @param request the HttpServletRequest.
   * @return String of data.
   */
  static String getDataAsString(HttpServletRequest request) throws IOException {
    var datasb = new StringBuilder();
    var s = "";
    while ((s = request.getReader().readLine()) != null) {
      datasb.append(s);
    }
    return datasb.toString();
  }


  /**
   * Returns a Map containing headers whose key is the header name and whose value(s) is/are the
   * header value(s).
   * 
   * <p>
   * Use this method to extract the headers from the request.
   * 
   * @param request the {@link HttpServletRequest}.
   * @return Map of the arguments.
   */
  static final Map<String, Object> getHeaders(HttpServletRequest request) {
    var headers = new HashMap<String, Object>();
    var headerNames = request.getHeaderNames();

    if (Objects.nonNull(headerNames)) {
      while (headerNames.hasMoreElements()) {
        var name = headerNames.nextElement();
        var headerValues = request.getHeaders(name);
        int valCount = 0;
        var valList = new ArrayList<String>();
        while (headerValues.hasMoreElements()) {
          valCount++;
          valList.add(headerValues.nextElement());
        }
        if (valCount == 1) {
          headers.put(name, valList.get(0));
        } else {
          headers.put(name, valList);
        }
      }
    }
    LOGGER.finer(() -> {
      return String.format("headers=%s", headers.toString());
    });
    return headers;
  }

  /**
   * Returns a Map containing string representation of request attributes whose keys are the
   * attribute names and whose values are the attributes values.
   * 
   * @param request the {@link HttpServletRequest}.
   * @return a Map of the request attributes.
   */
  static final Map<String, Object> getAttributes(HttpServletRequest request) {
    var attributes = new HashMap<String, Object>();
    var attributeNames = request.getAttributeNames();

    while (attributeNames.hasMoreElements()) {
      var name = attributeNames.nextElement();
      var attributeVal = request.getAttribute(name);
      if (attributeVal instanceof Iterable) {
        @SuppressWarnings("unchecked")
        var iterableAttrVal = (Iterable<Object>) attributeVal;
        var attrValsList = new ArrayList<Object>();
        for (Object val : iterableAttrVal) {
          attrValsList.add(val);
        }
        attributes.put(name, attrValsList);
      } else if (attributeVal instanceof Boolean || attributeVal instanceof Number) {
        attributes.put(name, attributeVal);
      } else {
        attributes.put(name, attributeVal.toString());
      }
    }
    LOGGER.finer(() -> {
      return String.format("attributes=%s", attributes.toString());
    });
    return attributes;
  }


  /**
   * Returns the url of the request.
   * 
   * <p>
   * If a query string is part of the request, it is appended to the url in the normal way.
   * 
   * @param request the HttpServletRequest.
   * @return url String.
   */
  static final String getRequestUrl(HttpServletRequest request) {
    var url = request.getRequestURL();
    if (request.getQueryString() != null && request.getQueryString().length() > 0) {
      url.append("?").append(request.getQueryString());
    }
    return url.toString();
  }
}
