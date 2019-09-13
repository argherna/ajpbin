package com.github.argherna.ajpbin;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

final class ServletApiProxies {

  /**
   * Empty constructor to prevent instantiation.
   */
  private ServletApiProxies() {
  }

  static HttpServletRequest createHttpServletRequestProxy(InvocationHandler ih) {
    return (HttpServletRequest) Proxy.newProxyInstance(ServletApiProxies.class.getClassLoader(),
        new Class<?>[] { HttpServletRequest.class }, ih);
  }

  static HttpServletResponse createHttpServletResponseProxy(InvocationHandler ih) {
    return (HttpServletResponse) Proxy.newProxyInstance(ServletApiProxies.class.getClassLoader(),
        new Class<?>[] { HttpServletResponse.class }, ih);
  }
}
