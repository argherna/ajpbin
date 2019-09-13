package com.github.argherna.ajpbin;

import static com.github.argherna.ajpbin.Constants.OUTPUT_DOCUMENT_ATTR_NAME;
import static com.github.argherna.ajpbin.Constants.OUTPUT_MAP_ATTR_NAME;
import static com.github.argherna.ajpbin.Responses.isError;
import static com.github.argherna.ajpbin.Xml.render;
import static javax.servlet.http.HttpServletResponse.SC_CREATED;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.Map;
import java.util.function.Consumer;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;

@WebFilter(servletNames = { "AjpMethodsServlet" })
public class ResponseHeadersFilter extends HttpFilter {

  @Override
  protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    chain.doFilter(request, response);

    AjpbinHeaders.setHeaders(response);

    if ((response.getStatus() != SC_NO_CONTENT && response.getStatus() != SC_CREATED)
        && !isError(response.getStatus())) {
      @SuppressWarnings("unchecked")
      var outputMap = (Map<String, Object>) request.getAttribute(OUTPUT_MAP_ATTR_NAME);
      if (outputMap != null) {
        writeJson(response, outputMap);
        return;
      }

      var outputDoc = (Document) request.getAttribute(OUTPUT_DOCUMENT_ATTR_NAME);
      if (outputDoc != null) {
        try {
          writeXml(response, outputDoc);
        } catch (TransformerException e) {
          throw new ServletException(e);
        }
        return;
      }

      // No output given when expected. Throw an exception.
      throw new ServletException("Expected some kind of output, but none was generated.");
    }
  }

  private void writeJson(HttpServletResponse response, Map<String, Object> outputMap) throws IOException {
    var json = Json.renderObject(outputMap);
    response.setContentType("application/json");
    response.setContentLength(json.length());

    var writer = response.getWriter();
    writer.print(json);
    writer.flush();
  }

  private void writeXml(HttpServletResponse response, Document outputDoc) throws IOException, TransformerException {
    writeXmlHeaders(response);
    var writer = response.getWriter();
    render(outputDoc, writer);
    writer.flush();
  }

  private void writeXml(HttpServletRequest request, HttpServletResponse response)
      throws XMLStreamException, FactoryConfigurationError, IOException {

    response.setContentType("text/xml");
    response.setHeader("Transfer-Encoding", "chunked");

    @SuppressWarnings("unchecked")
    var outputWriter = (Consumer<OutputStream>) request.getAttribute(Constants.OUTPUT_WRITER_ATTR_NAME);
    outputWriter.accept(response.getOutputStream());
  }

  private void writeXmlHeaders(HttpServletResponse response) {
    response.setContentType("text/xml");
    response.setHeader("Transfer-Encoding", "chunked");
  }
}
