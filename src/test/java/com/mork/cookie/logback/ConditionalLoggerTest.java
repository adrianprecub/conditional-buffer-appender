package com.mork.cookie.logback;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ConditionalLoggerTest {

    private ConditionalLogger conditionalLogger;
    private ConditionalLogger namedConditionalLogger;

    @BeforeEach
    void setUp() {
        conditionalLogger = new ConditionalLogger(ConditionalLoggerTest.class);
        namedConditionalLogger = new ConditionalLogger("test.named.logger");
    }

    @AfterEach
    void tearDown() {
        // Clear any request context that might have been set
        RequestLoggingContext.clear();
    }

    @Test
    void testConstructorWithClass() {
        assertNotNull(conditionalLogger);
        assertNotNull(conditionalLogger.getLogger());
        assertEquals("com.mork.cookie.logback.ConditionalLoggerTest", conditionalLogger.getLogger().getName());
    }

    @Test
    void testConstructorWithName() {
        assertNotNull(namedConditionalLogger);
        assertNotNull(namedConditionalLogger.getLogger());
        assertEquals("test.named.logger", namedConditionalLogger.getLogger().getName());
    }

    @Test
    void testGetLogger() {
        Logger underlyingLogger = conditionalLogger.getLogger();
        assertThat(underlyingLogger).isEqualTo(LoggerFactory.getLogger(ConditionalLoggerTest.class));
    }

    @Test
    void testDebugMethods() {
        // Test simple debug message
        conditionalLogger.debug("Debug message");
        
        // Test debug with parameters
        conditionalLogger.debug("Debug message with params: {} and {}", "param1", "param2");
        
        // Test debug with variable arguments
        conditionalLogger.debug("Debug message with varargs: {}, {}, {}", "arg1", "arg2", "arg3");
        
        // These should not throw exceptions and should delegate to underlying logger
        assertNotNull(conditionalLogger.getLogger());
    }

    @Test
    void testInfoMethods() {
        // Test simple info message
        conditionalLogger.info("Info message");
        
        // Test info with parameters
        conditionalLogger.info("Info message with params: {} and {}", "param1", "param2");
        
        // Test info with variable arguments
        conditionalLogger.info("Info message with varargs: {}, {}, {}", "arg1", "arg2", "arg3");
        
        // These should not throw exceptions and should delegate to underlying logger
        assertNotNull(conditionalLogger.getLogger());
    }

    @Test
    void testWarnMethods() {
        // Test simple warn message
        conditionalLogger.warn("Warn message");
        
        // Test warn with parameters
        conditionalLogger.warn("Warn message with params: {} and {}", "param1", "param2");
        
        // Test warn with variable arguments
        conditionalLogger.warn("Warn message with varargs: {}, {}, {}", "arg1", "arg2", "arg3");
        
        // These should not throw exceptions and should delegate to underlying logger
        assertNotNull(conditionalLogger.getLogger());
    }

    @Test
    void testErrorMethods() {
        // Test simple error message
        conditionalLogger.error("Error message");
        
        // Test error with parameters
        conditionalLogger.error("Error message with params: {} and {}", "param1", "param2");
        
        // Test error with variable arguments
        conditionalLogger.error("Error message with varargs: {}, {}, {}", "arg1", "arg2", "arg3");
        
        // These should not throw exceptions and should delegate to underlying logger
        assertNotNull(conditionalLogger.getLogger());
    }

    @Test
    void testErrorWithThrowable() {
        Exception testException = new RuntimeException("Test exception");
        
        // Test error with throwable
        conditionalLogger.error("Error occurred", testException);
        
        // This should not throw exception and should delegate to underlying logger
        assertNotNull(conditionalLogger.getLogger());
    }

    @Test
    void testLoggerDelegation() {
        // Verify that the ConditionalLogger properly delegates to the underlying SLF4J logger
        Logger underlyingLogger = conditionalLogger.getLogger();
        
        // Test that the underlying logger is actually from SLF4J
        assertThat(underlyingLogger).isInstanceOf(Logger.class);
        assertThat(underlyingLogger.getName()).isEqualTo(ConditionalLoggerTest.class.getName());
    }

    @Test
    void testMultipleLoggerInstances() {
        ConditionalLogger logger1 = new ConditionalLogger("logger1");
        ConditionalLogger logger2 = new ConditionalLogger("logger2");
        ConditionalLogger logger3 = new ConditionalLogger(String.class);
        
        assertThat(logger1.getLogger().getName()).isEqualTo("logger1");
        assertThat(logger2.getLogger().getName()).isEqualTo("logger2");
        assertThat(logger3.getLogger().getName()).isEqualTo("java.lang.String");
        
        // Verify they are different instances
        assertThat(logger1.getLogger()).isNotSameAs(logger2.getLogger());
        assertThat(logger2.getLogger()).isNotSameAs(logger3.getLogger());
    }

    @Test
    void testLoggingWithRequestContext() {
        // Set up request context
        String requestId = "test-request-123";
        RequestLoggingContext.setRequestId(requestId);
        
        // Log at different levels
        conditionalLogger.debug("Debug in request context");
        conditionalLogger.info("Info in request context");
        conditionalLogger.warn("Warn in request context");
        conditionalLogger.error("Error in request context");
        
        // Verify request context is still intact
        assertThat(RequestLoggingContext.getRequestId()).isEqualTo(requestId);
        
        // Clean up
        RequestLoggingContext.clear();
    }

    @Test
    void testParameterizedLogging() {
        // Test with null parameters
        conditionalLogger.debug("Debug with null: {}", (Object) null);
        conditionalLogger.info("Info with null: {}", (Object) null);
        conditionalLogger.warn("Warn with null: {}", (Object) null);
        conditionalLogger.error("Error with null: {}", (Object) null);
        
        // Test with mixed parameters
        conditionalLogger.debug("Mixed params: {} and {} and {}", 42, "string", true);
        conditionalLogger.info("Mixed params: {} and {} and {}", 42, "string", true);
        conditionalLogger.warn("Mixed params: {} and {} and {}", 42, "string", true);
        conditionalLogger.error("Mixed params: {} and {} and {}", 42, "string", true);
        
        // Should not throw exceptions
        assertNotNull(conditionalLogger.getLogger());
    }

    @Test
    void testEmptyAndNullMessages() {
        // Test with empty messages
        conditionalLogger.debug("");
        conditionalLogger.info("");
        conditionalLogger.warn("");
        conditionalLogger.error("");
        
        // Test with null messages (should be handled gracefully by underlying logger)
        conditionalLogger.debug(null);
        conditionalLogger.info(null);
        conditionalLogger.warn(null);
        conditionalLogger.error(null);
        
        // Should not throw exceptions
        assertNotNull(conditionalLogger.getLogger());
    }

    @Test
    void testErrorWithNullThrowable() {
        // Test error with null throwable
        conditionalLogger.error("Error with null throwable", (Throwable) null);
        
        // Should not throw exception
        assertNotNull(conditionalLogger.getLogger());
    }

    @Test
    void testLargeNumberOfParameters() {
        // Test with many parameters
        conditionalLogger.debug("Many params: {} {} {} {} {} {} {} {} {} {}", 
                1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        conditionalLogger.info("Many params: {} {} {} {} {} {} {} {} {} {}", 
                1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        conditionalLogger.warn("Many params: {} {} {} {} {} {} {} {} {} {}", 
                1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        conditionalLogger.error("Many params: {} {} {} {} {} {} {} {} {} {}", 
                1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        
        // Should not throw exceptions
        assertNotNull(conditionalLogger.getLogger());
    }
}