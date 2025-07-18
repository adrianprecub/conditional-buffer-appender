package com.mork.cookie.logback.spring;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import com.mork.cookie.logback.ConditionalBufferAppender;
import com.mork.cookie.logback.RequestLoggingFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConditionalLoggingConfigurationTest {

    @Mock
    private LoggerContext mockLoggerContext;

    @Mock
    private Logger mockRootLogger;

    @Mock
    private ConditionalBufferAppender mockExistingAppender;

    private ConditionalLoggingConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new ConditionalLoggingConfiguration();
    }

    @Test
    void testConditionalBufferAppenderWithExistingAppender() {
        // Mock LoggerFactory to return our mock context
        try (MockedStatic<LoggerFactory> loggerFactoryMock = mockStatic(LoggerFactory.class)) {
            loggerFactoryMock.when(LoggerFactory::getILoggerFactory).thenReturn(mockLoggerContext);
            
            // Mock the logger context to return our mock root logger
            when(mockLoggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).thenReturn(mockRootLogger);
            
            // Mock the root logger to return our existing appender
            when(mockRootLogger.getAppender("CONDITIONAL_BUFFER")).thenReturn(mockExistingAppender);
            
            ConditionalBufferAppender result = configuration.conditionalBufferAppender();
            
            assertThat(result).isEqualTo(mockExistingAppender);
        }
    }

    @Test
    void testConditionalBufferAppenderWithoutExistingAppender() {
        // Mock LoggerFactory to return our mock context
        try (MockedStatic<LoggerFactory> loggerFactoryMock = mockStatic(LoggerFactory.class)) {
            loggerFactoryMock.when(LoggerFactory::getILoggerFactory).thenReturn(mockLoggerContext);
            
            // Mock the logger context to return our mock root logger
            when(mockLoggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).thenReturn(mockRootLogger);
            
            // Mock the root logger to return null (no existing appender)
            when(mockRootLogger.getAppender("CONDITIONAL_BUFFER")).thenReturn(null);
            
            ConditionalBufferAppender result = configuration.conditionalBufferAppender();
            
            assertNotNull(result);
            assertThat(result.getName()).isEqualTo("CONDITIONAL_BUFFER");
            assertThat(result.getMaxBufferSize()).isEqualTo(1000);
            assertThat(result.getBufferTimeoutMinutes()).isEqualTo(10);
            assertThat(result.getCleanupIntervalMinutes()).isEqualTo(5);
            assertNotNull(result.getEncoder());
            assertTrue(result.isStarted());
        }
    }

    @Test
    void testRequestLoggingFilterConfiguration() {
        ConditionalBufferAppender appender = new ConditionalBufferAppender();
        appender.setName("TEST_APPENDER");
        
        // Create mock encoder
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(new LoggerContext());
        encoder.setPattern("%msg%n");
        encoder.start();
        appender.setEncoder(encoder);
        appender.setContext(new LoggerContext());
        appender.start();
        
        FilterRegistrationBean<RequestLoggingFilter> result = configuration.requestLoggingFilter(appender);
        
        assertNotNull(result);
        assertNotNull(result.getFilter());
        assertThat(result.getUrlPatterns()).contains("/*");
        assertThat(result.getOrder()).isEqualTo(1);
        
        // Clean up
        appender.stop();
        encoder.stop();
    }

    @Test
    void testRequestLoggingFilterWithNullAppender() {
        FilterRegistrationBean<RequestLoggingFilter> result = configuration.requestLoggingFilter(null);
        
        assertNotNull(result);
        assertNotNull(result.getFilter());
        assertThat(result.getUrlPatterns()).contains("/*");
        assertThat(result.getOrder()).isEqualTo(1);
    }

    @Test
    void testProgrammaticAppenderConfiguration() {
        LoggerContext realContext = new LoggerContext();
        
        // Mock LoggerFactory to return real context
        try (MockedStatic<LoggerFactory> loggerFactoryMock = mockStatic(LoggerFactory.class)) {
            loggerFactoryMock.when(LoggerFactory::getILoggerFactory).thenReturn(realContext);
            
            ConditionalBufferAppender result = configuration.conditionalBufferAppender();
            
            assertNotNull(result);
            assertThat(result.getName()).isEqualTo("CONDITIONAL_BUFFER");
            assertThat(result.getContext()).isEqualTo(realContext);
            assertThat(result.getMaxBufferSize()).isEqualTo(1000);
            assertThat(result.getBufferTimeoutMinutes()).isEqualTo(10);
            assertThat(result.getCleanupIntervalMinutes()).isEqualTo(5);
            
            // Verify encoder is configured
            assertNotNull(result.getEncoder());
            assertThat(result.getEncoder()).isInstanceOf(PatternLayoutEncoder.class);
            
            PatternLayoutEncoder encoder = (PatternLayoutEncoder) result.getEncoder();
            assertThat(encoder.getPattern()).isEqualTo("%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
            assertTrue(encoder.isStarted());
            
            assertTrue(result.isStarted());
            
            // Clean up
            result.stop();
        }
    }

    @Test
    void testFilterOrderAndUrlPatterns() {
        ConditionalBufferAppender appender = new ConditionalBufferAppender();
        FilterRegistrationBean<RequestLoggingFilter> filterBean = configuration.requestLoggingFilter(appender);
        
        // Test filter order
        assertEquals(1, filterBean.getOrder());
        
        // Test URL patterns
        assertThat(filterBean.getUrlPatterns()).hasSize(1);
        assertThat(filterBean.getUrlPatterns()).contains("/*");
        
        // Test filter type
        assertThat(filterBean.getFilter()).isInstanceOf(RequestLoggingFilter.class);
    }

    @Test
    void testConfigurationIsSpringConfiguration() {
        // Verify that the class has the @Configuration annotation
        assertTrue(ConditionalLoggingConfiguration.class.isAnnotationPresent(org.springframework.context.annotation.Configuration.class));
    }

    @Test
    void testAppenderEncoderPattern() {
        LoggerContext realContext = new LoggerContext();
        
        try (MockedStatic<LoggerFactory> loggerFactoryMock = mockStatic(LoggerFactory.class)) {
            loggerFactoryMock.when(LoggerFactory::getILoggerFactory).thenReturn(realContext);
            
            ConditionalBufferAppender result = configuration.conditionalBufferAppender();
            
            PatternLayoutEncoder encoder = (PatternLayoutEncoder) result.getEncoder();
            String expectedPattern = "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n";
            assertThat(encoder.getPattern()).isEqualTo(expectedPattern);
            
            // Clean up
            result.stop();
        }
    }

    @Test
    void testAppenderDefaultConfiguration() {
        LoggerContext realContext = new LoggerContext();
        
        try (MockedStatic<LoggerFactory> loggerFactoryMock = mockStatic(LoggerFactory.class)) {
            loggerFactoryMock.when(LoggerFactory::getILoggerFactory).thenReturn(realContext);
            
            ConditionalBufferAppender result = configuration.conditionalBufferAppender();
            
            // Test default configuration values
            assertEquals(1000, result.getMaxBufferSize());
            assertEquals(10, result.getBufferTimeoutMinutes());
            assertEquals(5, result.getCleanupIntervalMinutes());
            
            // Clean up
            result.stop();
        }
    }

    @Test
    void testBeanMethodsAreAnnotated() throws NoSuchMethodException {
        // Verify that bean methods have @Bean annotation
        assertTrue(ConditionalLoggingConfiguration.class
                .getMethod("conditionalBufferAppender")
                .isAnnotationPresent(org.springframework.context.annotation.Bean.class));
        
        assertTrue(ConditionalLoggingConfiguration.class
                .getMethod("requestLoggingFilter", ConditionalBufferAppender.class)
                .isAnnotationPresent(org.springframework.context.annotation.Bean.class));
    }

    @Test
    void testMultipleAppenderCreation() {
        LoggerContext realContext = new LoggerContext();
        
        try (MockedStatic<LoggerFactory> loggerFactoryMock = mockStatic(LoggerFactory.class)) {
            loggerFactoryMock.when(LoggerFactory::getILoggerFactory).thenReturn(realContext);
            
            // Create multiple appenders
            ConditionalBufferAppender appender1 = configuration.conditionalBufferAppender();
            ConditionalBufferAppender appender2 = configuration.conditionalBufferAppender();
            
            // Both should be valid but different instances
            assertNotNull(appender1);
            assertNotNull(appender2);
            assertThat(appender1).isNotSameAs(appender2);
            
            // Both should have the same configuration
            assertEquals(appender1.getName(), appender2.getName());
            assertEquals(appender1.getMaxBufferSize(), appender2.getMaxBufferSize());
            assertEquals(appender1.getBufferTimeoutMinutes(), appender2.getBufferTimeoutMinutes());
            assertEquals(appender1.getCleanupIntervalMinutes(), appender2.getCleanupIntervalMinutes());
            
            // Clean up
            appender1.stop();
            appender2.stop();
        }
    }

    @Test
    void testFilterRegistrationBeanConfiguration() {
        ConditionalBufferAppender appender = new ConditionalBufferAppender();
        FilterRegistrationBean<RequestLoggingFilter> filterBean = configuration.requestLoggingFilter(appender);
        
        // Test that it's properly configured
        assertNotNull(filterBean.getFilter());
        assertThat(filterBean.getFilter()).isInstanceOf(RequestLoggingFilter.class);
        
        // Test URL patterns
        assertThat(filterBean.getUrlPatterns()).contains("/*");
        
        // Test order
        assertEquals(1, filterBean.getOrder());
        
        // Test that filter is enabled by default
        assertTrue(filterBean.isEnabled());
    }
}