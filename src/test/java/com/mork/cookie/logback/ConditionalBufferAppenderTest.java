package com.mork.cookie.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.encoder.EchoEncoder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class ConditionalBufferAppenderTest {

    private ConditionalBufferAppender appender;
    private LoggerContext loggerContext;
    private Logger logger;
    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
        loggerContext = new LoggerContext();
        logger = loggerContext.getLogger("test");
        
        appender = new ConditionalBufferAppender();
        appender.setContext(loggerContext);
        appender.setName("TEST_APPENDER");
        
        EchoEncoder<ch.qos.logback.classic.spi.ILoggingEvent> encoder = new EchoEncoder<>();
        encoder.setContext(loggerContext);
        encoder.start();
        appender.setEncoder(encoder);
        
        appender.start();
        
        // Capture System.out
        outputStream = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
        
        // Clear any existing context
        RequestLoggingContext.clear();
    }

    @AfterEach
    void tearDown() {
        if (appender != null) {
            appender.stop();
        }
        RequestLoggingContext.clear();
        System.setOut(originalOut);
    }

    @Test
    void testAppenderStartAndStop() {
        assertTrue(appender.isStarted());
        
        appender.stop();
        assertFalse(appender.isStarted());
    }

    @Test
    void testAppenderWithoutEncoder() {
        ConditionalBufferAppender appenderWithoutEncoder = new ConditionalBufferAppender();
        appenderWithoutEncoder.setContext(loggerContext);
        appenderWithoutEncoder.start();
        
        assertFalse(appenderWithoutEncoder.isStarted());
    }

    @Test
    void testLogEventWithoutRequestContext() {
        LoggingEvent event = new LoggingEvent("test.class", logger, Level.INFO, "Test message", null, null);
        
        appender.append(event);
        
        // Should not buffer since no request context
        assertThat(appender.getRequestBuffers()).isEmpty();
    }

    @Test
    void testLogEventWithoutRequestContextButError() {
        LoggingEvent event = new LoggingEvent("test.class", logger, Level.ERROR, "Error message", null, null);
        
        appender.append(event);
        
        // Should write to console immediately for ERROR level
        String output = outputStream.toString();
        assertThat(output).contains("Error message");
    }

    @Test
    void testLogEventBuffering() {
        String requestId = "test-request-123";
        RequestLoggingContext.setRequestId(requestId);
        
        LoggingEvent debugEvent = new LoggingEvent("test.class", logger, Level.DEBUG, "Debug message", null, null);
        LoggingEvent infoEvent = new LoggingEvent("test.class", logger, Level.INFO, "Info message", null, null);
        
        appender.append(debugEvent);
        appender.append(infoEvent);
        
        Map<String, ConditionalBufferAppender.RequestLogBuffer> buffers = appender.getRequestBuffers();
        assertThat(buffers).hasSize(1);
        assertThat(buffers.get(requestId).getEvents()).hasSize(2);
        
        // Should not have written to console yet
        assertThat(outputStream.toString()).isEmpty();
    }

    @Test
    void testErrorEventMarksRequest() {
        String requestId = "test-request-456";
        RequestLoggingContext.setRequestId(requestId);
        
        LoggingEvent errorEvent = new LoggingEvent("test.class", logger, Level.ERROR, "Error occurred", null, null);
        
        appender.append(errorEvent);
        
        assertTrue(RequestLoggingContext.hasError());
        assertThat(appender.getRequestBuffers().get(requestId).getEvents()).hasSize(1);
    }

    @Test
    void testFlushRequestLogsWithError() {
        String requestId = "test-request-789";
        RequestLoggingContext.setRequestId(requestId);
        
        LoggingEvent debugEvent = new LoggingEvent("test.class", logger, Level.DEBUG, "Debug message", null, null);
        LoggingEvent infoEvent = new LoggingEvent("test.class", logger, Level.INFO, "Info message", null, null);
        LoggingEvent errorEvent = new LoggingEvent("test.class", logger, Level.ERROR, "Error occurred", null, null);
        
        appender.append(debugEvent);
        appender.append(infoEvent);
        appender.append(errorEvent);
        
        RequestLoggingContext.markError();
        appender.flushRequestLogsIfError(requestId);
        
        String output = outputStream.toString();
        assertThat(output).contains("Debug message");
        assertThat(output).contains("Info message");
        assertThat(output).contains("Error occurred");
        assertThat(output).contains("REQUEST COMPLETED WITH ERROR");
        
        // Buffer should be removed after flushing
        assertThat(appender.getRequestBuffers()).doesNotContainKey(requestId);
    }

    @Test
    void testFlushRequestLogsWithoutError() {
        String requestId = "test-request-success";
        RequestLoggingContext.setRequestId(requestId);
        
        LoggingEvent debugEvent = new LoggingEvent("test.class", logger, Level.DEBUG, "Debug message", null, null);
        LoggingEvent infoEvent = new LoggingEvent("test.class", logger, Level.INFO, "Info message", null, null);
        LoggingEvent warnEvent = new LoggingEvent("test.class", logger, Level.WARN, "Warn message", null, null);
        
        appender.append(debugEvent);
        appender.append(infoEvent);
        appender.append(warnEvent);
        
        appender.flushRequestLogsIfError(requestId);
        
        String output = outputStream.toString();
        assertThat(output).contains("Info message");
        assertThat(output).contains("REQUEST COMPLETED SUCCESSFULLY");
        assertThat(output).doesNotContain("Debug message");
        assertThat(output).doesNotContain("Warn message");
        
        // Buffer should be removed after flushing
        assertThat(appender.getRequestBuffers()).doesNotContainKey(requestId);
    }

    @Test
    void testMaxBufferSizeLimit() {
        String requestId = "test-request-limit";
        RequestLoggingContext.setRequestId(requestId);
        
        appender.setMaxBufferSize(2);
        
        LoggingEvent event1 = new LoggingEvent("test.class", logger, Level.INFO, "Message 1", null, null);
        LoggingEvent event2 = new LoggingEvent("test.class", logger, Level.INFO, "Message 2", null, null);
        LoggingEvent event3 = new LoggingEvent("test.class", logger, Level.INFO, "Message 3", null, null);
        
        appender.append(event1);
        appender.append(event2);
        appender.append(event3); // Should be dropped
        
        ConditionalBufferAppender.RequestLogBuffer buffer = appender.getRequestBuffers().get(requestId);
        assertThat(buffer.getEvents()).hasSize(2);
    }

    @Test
    void testCleanupRequest() {
        String requestId = "test-cleanup";
        RequestLoggingContext.setRequestId(requestId);
        
        LoggingEvent event = new LoggingEvent("test.class", logger, Level.INFO, "Test message", null, null);
        appender.append(event);
        
        assertThat(appender.getRequestBuffers()).containsKey(requestId);
        
        appender.cleanupRequest(requestId);
        
        assertThat(appender.getRequestBuffers()).doesNotContainKey(requestId);
    }

    @Test
    void testForceCleanupAll() {
        String requestId1 = "test-cleanup-1";
        String requestId2 = "test-cleanup-2";
        
        RequestLoggingContext.setRequestId(requestId1);
        LoggingEvent event1 = new LoggingEvent("test.class", logger, Level.INFO, "Message 1", null, null);
        appender.append(event1);
        
        RequestLoggingContext.setRequestId(requestId2);
        LoggingEvent event2 = new LoggingEvent("test.class", logger, Level.INFO, "Message 2", null, null);
        appender.append(event2);
        
        assertThat(appender.getRequestBuffers()).hasSize(2);
        
        appender.forceCleanupAll();
        
        assertThat(appender.getRequestBuffers()).isEmpty();
    }

    @Test
    void testRequestLogBufferExpiration() throws InterruptedException {
        ConditionalBufferAppender.RequestLogBuffer buffer = new ConditionalBufferAppender.RequestLogBuffer();
        
        assertFalse(buffer.isExpired(1000));
        
        Thread.sleep(50);
        
        assertTrue(buffer.isExpired(10)); // 10ms timeout
        assertFalse(buffer.isExpired(1000)); // 1000ms timeout
    }

    @Test
    void testRequestLogBufferAccessTimeUpdate() {
        ConditionalBufferAppender.RequestLogBuffer buffer = new ConditionalBufferAppender.RequestLogBuffer();
        long initialTime = buffer.getLastAccessTime();
        
        LoggingEvent event = new LoggingEvent("test.class", logger, Level.INFO, "Test", null, null);
        buffer.addEvent(event);
        
        assertTrue(buffer.getLastAccessTime() >= initialTime);
        
        long timeAfterAdd = buffer.getLastAccessTime();
        List<ch.qos.logback.classic.spi.ILoggingEvent> events = buffer.getEvents();
        
        assertTrue(buffer.getLastAccessTime() >= timeAfterAdd);
        assertThat(events).hasSize(1);
    }

    @Test
    void testConfigurationProperties() {
        assertEquals(1000, appender.getMaxBufferSize());
        assertEquals(10, appender.getBufferTimeoutMinutes());
        assertEquals(5, appender.getCleanupIntervalMinutes());
        
        appender.setMaxBufferSize(500);
        appender.setBufferTimeoutMinutes(15);
        appender.setCleanupIntervalMinutes(3);
        
        assertEquals(500, appender.getMaxBufferSize());
        assertEquals(15, appender.getBufferTimeoutMinutes());
        assertEquals(3, appender.getCleanupIntervalMinutes());
        
        // Test minimum values
        appender.setMaxBufferSize(0);
        appender.setBufferTimeoutMinutes(0);
        appender.setCleanupIntervalMinutes(0);
        
        assertEquals(1, appender.getMaxBufferSize());
        assertEquals(1, appender.getBufferTimeoutMinutes());
        assertEquals(1, appender.getCleanupIntervalMinutes());
    }

    @Test
    void testEncoderConfiguration() {
        assertNotNull(appender.getEncoder());
        
        EchoEncoder<ch.qos.logback.classic.spi.ILoggingEvent> newEncoder = new EchoEncoder<>();
        appender.setEncoder(newEncoder);
        
        assertEquals(newEncoder, appender.getEncoder());
    }

    @Test
    void testFlushWithNonExistentRequestId() {
        appender.flushRequestLogsIfError("non-existent-request");
        
        // Should not throw exception and output should be empty
        assertThat(outputStream.toString()).isEmpty();
    }

    @Test
    void testCleanupExpiredRequestsWithValidBuffers() {
        String requestId = "test-cleanup-expired";
        RequestLoggingContext.setRequestId(requestId);
        
        LoggingEvent event = new LoggingEvent("test.class", logger, Level.INFO, "Test message", null, null);
        appender.append(event);
        
        // Buffer should exist and not be expired
        assertThat(appender.getRequestBuffers()).hasSize(1);
        
        appender.cleanupExpiredRequests();
        
        // Buffer should still exist as it's not expired
        assertThat(appender.getRequestBuffers()).hasSize(1);
    }
}