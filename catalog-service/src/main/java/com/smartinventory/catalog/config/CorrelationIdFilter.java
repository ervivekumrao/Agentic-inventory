package com.smartinventory.catalog.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter that establishes and propagates a unique Correlation ID (MDC)
 * across distributed requests for end-to-end observability and log aggregation.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter implements Filter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String MDC_CORRELATION_ID_KEY = "correlationId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest httpRequest && response instanceof HttpServletResponse httpResponse) {
            String correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER);
            if (correlationId == null || correlationId.trim().isEmpty()) {
                correlationId = UUID.randomUUID().toString();
            }

            MDC.put(MDC_CORRELATION_ID_KEY, correlationId);
            httpResponse.setHeader(CORRELATION_ID_HEADER, correlationId);

            try {
                chain.doFilter(request, response);
            } finally {
                MDC.remove(MDC_CORRELATION_ID_KEY);
            }
        } else {
            chain.doFilter(request, response);
        }
    }
}
