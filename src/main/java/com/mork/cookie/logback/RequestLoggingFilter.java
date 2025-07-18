package com.mork.cookie.logback;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;

/**
 * Servlet filter that manages request-scoped logging context.
 * Sets up request ID at the beginning of request processing and
 * triggers conditional log flushing at the end.
 */
public class RequestLoggingFilter implements Filter {

    private final ConditionalBufferAppender appender;

    /**
     * Creates a new RequestLoggingFilter with the specified appender.
     * 
     * @param appender the ConditionalBufferAppender to use for log flushing
     */
    public RequestLoggingFilter(ConditionalBufferAppender appender) {
        this.appender = appender;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest) {
            String requestId = RequestLoggingContext.generateRequestId();
            RequestLoggingContext.setRequestId(requestId);

            try {
                // Process the request normally - errors will be marked but not flushed yet
                chain.doFilter(request, response);
            } finally {
                // After request is completely finished, check if there was an error and flush if needed
                if (appender != null) {
                    try {
                        appender.flushRequestLogsIfError(requestId);
                    } catch (Exception e) {
                        // Log the exception but don't propagate it
                        System.err.println("Error flushing request logs: " + e.getMessage());
                    }
                }

                // Clean up the request context
                RequestLoggingContext.clear();
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Initialize if needed
    }

    @Override
    public void destroy() {
        // Cleanup if needed
    }
}