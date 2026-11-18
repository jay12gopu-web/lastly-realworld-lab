package com.lastly.lab.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.UUID;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String supplied = request.getHeader("X-Correlation-ID");
        String id = supplied != null && supplied.matches("[A-Za-z0-9_-]{1,80}")
            ? supplied : UUID.randomUUID().toString();
        MDC.put("requestId", id);
        response.setHeader("X-Correlation-ID", id);
        try { chain.doFilter(request, response); }
        finally { MDC.remove("requestId"); }
    }
}
