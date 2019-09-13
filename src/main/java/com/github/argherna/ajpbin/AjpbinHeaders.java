package com.github.argherna.ajpbin;

import java.util.Date;

import javax.servlet.http.HttpServletResponse;

final class AjpbinHeaders {
    private AjpbinHeaders() {
    }

    static void setHeaders(HttpServletResponse response) {
        response.setDateHeader("Date", new Date().getTime());
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("X-XSS-Protection", "1; mode=block");
    }
}