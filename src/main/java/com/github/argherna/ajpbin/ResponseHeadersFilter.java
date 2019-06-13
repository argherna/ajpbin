package com.github.argherna.ajpbin;

import static com.github.argherna.ajpbin.Constants.OUTPUT_MAP_ATTR_NAME;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebFilter(servletNames = {"AjpMethodsServlet"})
public class ResponseHeadersFilter extends HttpFilter {

  @Override
  protected void doFilter(HttpServletRequest request, HttpServletResponse response,
      FilterChain chain) throws IOException, ServletException {

    chain.doFilter(request, response);

    @SuppressWarnings("unchecked")
    var outputMap = (Map<String, Object>) request.getAttribute(OUTPUT_MAP_ATTR_NAME);
    if (outputMap == null) {
      throw new ServletException("output data is null!");
    }
    var json = Json.renderObject(outputMap);
    response.setDateHeader("Date", new Date().getTime());
    response.setContentType("application/json");
    response.setContentLength(json.length());
    response.setHeader("X-Content-Type-Options", "nosniff");
    response.setHeader("X-Frame-Options", "DENY");
    response.setHeader("X-XSS-Protection", "1; mode=block");

    var writer = response.getWriter();
    writer.print(json);
    writer.flush();
  }
}
