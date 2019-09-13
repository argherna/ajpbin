package com.github.argherna.ajpbin;

import static javax.servlet.http.HttpServletResponse.SC_OK;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

class SerlvetApiInvocationHandler implements InvocationHandler {

  private final String contextPath;

  private final String methodName;

  private final Map<String, List<String>> parameters;

  private final String requestURI;

  private final Map<String, List<String>> requestHeaders;

  private final String requestBodyText;

  private final Map<String, Object> requestAttributes;

  private final Map<String, Object> responseHeaders;

  private int sendErrorCallCount = 0;

  private int setAttributeCallCount = 0;

  private int setStatusCallCount = 0;

  private int statusCode = SC_OK;

  private String statusDescription = "";

  static class Builder {

    private String contextPath = "";

    private String method = "";

    private String requestURI = "";

    private Map<String, Object> requestAttributes = new HashMap<>();

    private Map<String, List<String>> requestHeaders = Map.of();

    private String requestBodyText = "";

    private Map<String, List<String>> parameters = Map.of();

    Builder contextPath(String contextPath) {
      this.contextPath = contextPath;
      return this;
    }

    Builder method(String method) {
      this.method = method;
      return this;
    }

    Builder requestURI(String requestURI) {
      this.requestURI = requestURI;
      return this;
    }

    Builder requestHeaders(Map<String, List<String>> requestHeaders) {
      this.requestHeaders = requestHeaders;
      return this;
    }

    Builder requestBodyText(String requestBodyText) {
      this.requestBodyText = requestBodyText;
      return this;
    }

    Builder parameters(Map<String, List<String>> parameters) {
      this.parameters = parameters;
      return this;
    }

    Builder requestAttributes(Map<String, Object> requestAttributes) {
      this.requestAttributes = requestAttributes;
      return this;
    }

    SerlvetApiInvocationHandler build() {
      return new SerlvetApiInvocationHandler(this);
    }
  }

  private SerlvetApiInvocationHandler(Builder builder) {
    this.contextPath = builder.contextPath;
    this.methodName = builder.method;
    this.requestURI = builder.requestURI;
    this.requestHeaders = builder.requestHeaders;
    this.requestBodyText = builder.requestBodyText;
    this.requestAttributes = builder.requestAttributes;
    this.parameters = builder.parameters;
    this.responseHeaders = new HashMap<>();
  }

  static Builder builder() {
    return new SerlvetApiInvocationHandler.Builder();
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

    if (method.getName().equals("getAttribute")) {
      return requestAttributes.get((String) args[0]);
    }

    if (method.getName().equals("getContextPath")) {
      return contextPath;
    }

    if (method.getName().equals("getHeader")) {
      if (requestHeaders.isEmpty()) {
        return null;
      }
      var values = requestHeaders.get((String) args[0]);
      if (values != null && !values.isEmpty()) {
        return values.get(0);
      } else {
        return null;
      }
    }

    if (method.getName().equals("getInputStream")) {
      if (requestBodyText.length() > 0) {
        return new ByteArrayServletInputStream(new ByteArrayInputStream(requestBodyText.getBytes()));
      } else {
        return new ByteArrayServletInputStream(new ByteArrayInputStream(new byte[0]));
      }
    }

    if (method.getName().equals("getMethod")) {
      return methodName;
    }

    if (method.getName().equals("getOutputStream")) {
      return new ByteArrayServletOutputStream(new ByteArrayOutputStream());
    }

    if (method.getName().equals("getParameter")) {
      String nm = (String) args[0];
      if (nm == null || nm.isEmpty()) {
        return null;
      }
      if (parameters.containsKey(nm)) {
        List<String> values = parameters.get(nm);
        if (!values.isEmpty()) {
          return values.get(0);
        }
        return null;
      } else {
        return null;
      }
    }

    if (method.getName().equals("getRequestURI")) {
      return requestURI;
    }

    if (method.getName().equals("sendError")) {
      statusCode = (Integer) args[0];
      if (args.length > 1) {
        statusDescription = (String) args[1];
      }
      sendErrorCallCount++;
      return null;
    }

    if (method.getName().equals("setAttribute")) {
      requestAttributes.put((String) args[0], (Object) args[1]);
      setAttributeCallCount++;
      return null;
    }

    if (method.getName().equals("setContentType")) {
      responseHeaders.put("Content-Type", (String) args[0]);
      return null;
    }

    if (method.getName().equals("setDateHeader")) {
      responseHeaders.put((String) args[0], (Long) args[1]);
      return null;
    }

    if (method.getName().equals("setHeader")) {
      responseHeaders.put((String) args[0], (String) args[1]);
      return null;
    }

    if (method.getName().equals("setStatus")) {
      statusCode = (Integer) args[0];
      setStatusCallCount++;
      return null;
    }

    throw new NoSuchMethodException(method.getName() + " not supported!");
  }

  int getStatusCode() {
    return statusCode;
  }

  String getStatusDescription() {
    return statusDescription;
  }

  int getSendErrorCallCount() {
    return sendErrorCallCount;
  }

  int getSetAttributeCallCount() {
    return setAttributeCallCount;
  }

  int getSetStatusCallCount() {
    return setStatusCallCount;
  }

  Map<String, Object> getRequestAttributes() {
    return Map.copyOf(requestAttributes);
  }

  private static class ByteArrayServletInputStream extends ServletInputStream {

    private final ByteArrayInputStream bais;

    private ByteArrayServletInputStream(ByteArrayInputStream bais) {
      this.bais = bais;
    }

    @Override
    public boolean isFinished() {
      return false;
    }

    @Override
    public boolean isReady() {
      return true;
    }

    @Override
    public void setReadListener(ReadListener readListener) {

    }

    @Override
    public int read() throws IOException {
      return bais.read();
    }
  }

  private static class ByteArrayServletOutputStream extends ServletOutputStream {

    private final ByteArrayOutputStream baos;

    private ByteArrayServletOutputStream(ByteArrayOutputStream baos) {
      this.baos = baos;
    }

    @Override
    public boolean isReady() {
      return false;
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {

    }

    @Override
    public void write(int b) throws IOException {
      baos.write(b);
    }

  }
}
