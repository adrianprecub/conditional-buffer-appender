package com.mork.cookie.logback;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestLoggingFilterTest {

    @Mock
    private ConditionalBufferAppender mockAppender;

    @Mock
    private HttpServletRequest mockHttpRequest;

    @Mock
    private HttpServletResponse mockHttpResponse;

    @Mock
    private ServletRequest mockNonHttpRequest;

    @Mock
    private ServletResponse mockNonHttpResponse;

    @Mock
    private FilterChain mockFilterChain;

    @Mock
    private FilterConfig mockFilterConfig;

    private RequestLoggingFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RequestLoggingFilter(mockAppender);
        RequestLoggingContext.clear();
    }

    @AfterEach
    void tearDown() {
        RequestLoggingContext.clear();
    }

    @Test
    void testConstructor() {
        assertNotNull(filter);
        
        // Test with null appender
        RequestLoggingFilter nullAppenderFilter = new RequestLoggingFilter(null);
        assertNotNull(nullAppenderFilter);
    }

    @Test
    void testInitAndDestroy() throws ServletException {
        // init should not throw exception
        filter.init(mockFilterConfig);
        
        // destroy should not throw exception
        filter.destroy();
    }

    @Test
    void testDoFilterWithHttpRequest() throws IOException, ServletException {
        // Initially no request context
        assertNull(RequestLoggingContext.getRequestId());
        assertFalse(RequestLoggingContext.hasError());
        
        filter.doFilter(mockHttpRequest, mockHttpResponse, mockFilterChain);
        
        // Verify filter chain was called
        verify(mockFilterChain).doFilter(mockHttpRequest, mockHttpResponse);
        
        // Verify appender was called to flush logs
        verify(mockAppender).flushRequestLogsIfError(anyString());
        
        // Context should be cleared after filter
        assertNull(RequestLoggingContext.getRequestId());
        assertFalse(RequestLoggingContext.hasError());
    }

    @Test
    void testDoFilterWithNonHttpRequest() throws IOException, ServletException {
        // Initially no request context
        assertNull(RequestLoggingContext.getRequestId());
        
        filter.doFilter(mockNonHttpRequest, mockNonHttpResponse, mockFilterChain);
        
        // Verify filter chain was called
        verify(mockFilterChain).doFilter(mockNonHttpRequest, mockNonHttpResponse);
        
        // Verify appender was NOT called since it's not an HTTP request
        verify(mockAppender, never()).flushRequestLogsIfError(anyString());
        
        // Context should remain null
        assertNull(RequestLoggingContext.getRequestId());
    }

    @Test
    void testDoFilterWithNullAppender() throws IOException, ServletException {
        RequestLoggingFilter filterWithNullAppender = new RequestLoggingFilter(null);
        
        filterWithNullAppender.doFilter(mockHttpRequest, mockHttpResponse, mockFilterChain);
        
        // Verify filter chain was called
        verify(mockFilterChain).doFilter(mockHttpRequest, mockHttpResponse);
        
        // Should not throw exception even with null appender
        // Context should be cleared
        assertNull(RequestLoggingContext.getRequestId());
        assertFalse(RequestLoggingContext.hasError());
    }

    @Test
    void testDoFilterWithExceptionInChain() throws IOException, ServletException {
        ServletException testException = new ServletException("Test exception");
        doThrow(testException).when(mockFilterChain).doFilter(any(), any());
        
        try {
            filter.doFilter(mockHttpRequest, mockHttpResponse, mockFilterChain);
        } catch (ServletException e) {
            assertThat(e).isEqualTo(testException);
        }
        
        // Even with exception, appender should be called and context cleared
        verify(mockAppender).flushRequestLogsIfError(anyString());
        assertNull(RequestLoggingContext.getRequestId());
        assertFalse(RequestLoggingContext.hasError());
    }

    @Test
    void testDoFilterWithIOExceptionInChain() throws IOException, ServletException {
        IOException testException = new IOException("Test IO exception");
        doThrow(testException).when(mockFilterChain).doFilter(any(), any());
        
        try {
            filter.doFilter(mockHttpRequest, mockHttpResponse, mockFilterChain);
        } catch (IOException e) {
            assertThat(e).isEqualTo(testException);
        }
        
        // Even with exception, appender should be called and context cleared
        verify(mockAppender).flushRequestLogsIfError(anyString());
        assertNull(RequestLoggingContext.getRequestId());
        assertFalse(RequestLoggingContext.hasError());
    }

    @Test
    void testDoFilterWithRuntimeExceptionInChain() throws IOException, ServletException {
        RuntimeException testException = new RuntimeException("Test runtime exception");
        doThrow(testException).when(mockFilterChain).doFilter(any(), any());
        
        try {
            filter.doFilter(mockHttpRequest, mockHttpResponse, mockFilterChain);
        } catch (RuntimeException e) {
            assertThat(e).isEqualTo(testException);
        }
        
        // Even with exception, appender should be called and context cleared
        verify(mockAppender).flushRequestLogsIfError(anyString());
        assertNull(RequestLoggingContext.getRequestId());
        assertFalse(RequestLoggingContext.hasError());
    }

    @Test
    void testRequestContextSetupDuringFilter() throws IOException, ServletException {
        filter.doFilter(mockHttpRequest, mockHttpResponse, (request, response) -> {
            // During filter chain execution, request context should be set
            String requestId = RequestLoggingContext.getRequestId();
            assertNotNull(requestId);
            assertThat(requestId).isNotEmpty();
            assertFalse(RequestLoggingContext.hasError());
            
            // Mark error during processing
            RequestLoggingContext.markError();
            assertTrue(RequestLoggingContext.hasError());
        });
        
        // After filter, context should be cleared
        assertNull(RequestLoggingContext.getRequestId());
        assertFalse(RequestLoggingContext.hasError());
        
        // Verify appender was called
        verify(mockAppender).flushRequestLogsIfError(anyString());
    }

    @Test
    void testMultipleRequestsGenerateUniqueIds() throws IOException, ServletException {
        String[] capturedRequestIds = new String[2];
        
        // First request
        filter.doFilter(mockHttpRequest, mockHttpResponse, (request, response) -> {
            capturedRequestIds[0] = RequestLoggingContext.getRequestId();
        });
        
        // Second request
        filter.doFilter(mockHttpRequest, mockHttpResponse, (request, response) -> {
            capturedRequestIds[1] = RequestLoggingContext.getRequestId();
        });
        
        // Request IDs should be different
        assertNotNull(capturedRequestIds[0]);
        assertNotNull(capturedRequestIds[1]);
        assertThat(capturedRequestIds[0]).isNotEqualTo(capturedRequestIds[1]);
        
        // Both should be valid UUIDs
        assertThat(capturedRequestIds[0]).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        assertThat(capturedRequestIds[1]).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    void testFilterWithNestedFilterChain() throws IOException, ServletException {
        // Test with nested filter chain calls
        filter.doFilter(mockHttpRequest, mockHttpResponse, (request, response) -> {
            String outerRequestId = RequestLoggingContext.getRequestId();
            
            // Simulate nested filter processing
            filter.doFilter(mockHttpRequest, mockHttpResponse, (innerRequest, innerResponse) -> {
                String innerRequestId = RequestLoggingContext.getRequestId();
                
                // Inner request should have a different ID
                assertNotNull(innerRequestId);
                assertThat(innerRequestId).isNotEqualTo(outerRequestId);
            });
            
            // After inner filter, context should be cleared
            assertNull(RequestLoggingContext.getRequestId());
        });
        
        // Final context should be cleared
        assertNull(RequestLoggingContext.getRequestId());
        assertFalse(RequestLoggingContext.hasError());
    }

    @Test
    void testAppenderExceptionHandling() throws IOException, ServletException {
        // Mock appender to throw exception
        doThrow(new RuntimeException("Appender error")).when(mockAppender).flushRequestLogsIfError(anyString());
        
        // Filter should still work and not propagate appender exceptions
        filter.doFilter(mockHttpRequest, mockHttpResponse, mockFilterChain);
        
        // Filter chain should still be called
        verify(mockFilterChain).doFilter(mockHttpRequest, mockHttpResponse);
        
        // Context should still be cleared
        assertNull(RequestLoggingContext.getRequestId());
        assertFalse(RequestLoggingContext.hasError());
    }

    @Test
    void testFilterWithLongProcessingTime() throws IOException, ServletException {
        filter.doFilter(mockHttpRequest, mockHttpResponse, (request, response) -> {
            // Simulate long processing time
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Request context should still be valid
            assertNotNull(RequestLoggingContext.getRequestId());
            assertFalse(RequestLoggingContext.hasError());
        });
        
        // After processing, context should be cleared
        assertNull(RequestLoggingContext.getRequestId());
        verify(mockAppender).flushRequestLogsIfError(anyString());
    }

    @Test
    void testFilterWithErrorDuringProcessing() throws IOException, ServletException {
        filter.doFilter(mockHttpRequest, mockHttpResponse, (request, response) -> {
            // Mark error during processing
            RequestLoggingContext.markError();
            assertTrue(RequestLoggingContext.hasError());
        });
        
        // After processing, context should be cleared
        assertNull(RequestLoggingContext.getRequestId());
        assertFalse(RequestLoggingContext.hasError());
        
        // Appender should be called
        verify(mockAppender).flushRequestLogsIfError(anyString());
    }

    @Test
    void testFilterRequestTypeCasting() throws IOException, ServletException {
        // Test that filter properly handles request type casting
        filter.doFilter(mockNonHttpRequest, mockNonHttpResponse, mockFilterChain);
        
        // Should pass through without setting request context
        verify(mockFilterChain).doFilter(mockNonHttpRequest, mockNonHttpResponse);
        verify(mockAppender, never()).flushRequestLogsIfError(anyString());
        
        // Context should remain null
        assertNull(RequestLoggingContext.getRequestId());
        assertFalse(RequestLoggingContext.hasError());
    }
}