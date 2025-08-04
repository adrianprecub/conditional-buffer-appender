package com.mork.cookie.logback.micronaut;

import com.mork.cookie.logback.ConditionalBufferAppender;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Micronaut configuration factory for the Conditional Buffer Appender.
 * This configuration class sets up the necessary beans for request-scoped
 * conditional logging in a Micronaut application.
 */
@Factory
public class ConditionalLoggingConfiguration {

    /**
     * Creates and configures the ConditionalBufferAppender bean.
     * First tries to get an existing appender from logback configuration,
     * then falls back to programmatic creation if not found.
     * 
     * @return the configured ConditionalBufferAppender
     */
    @Bean
    @Singleton
    public ConditionalBufferAppender conditionalBufferAppender() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        // Try to get existing appender from logback configuration
        ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        ConditionalBufferAppender appender = (ConditionalBufferAppender) rootLogger.getAppender("CONDITIONAL_BUFFER");

        if (appender != null) {
            return appender;
        }

        // Fallback: create programmatically if not found in config
        appender = new ConditionalBufferAppender();
        appender.setName("CONDITIONAL_BUFFER");
        appender.setContext(loggerContext);

        // Set default configuration
        appender.setMaxBufferSize(1000);
        appender.setBufferTimeoutMinutes(10);
        appender.setCleanupIntervalMinutes(5);

        // Create and set encoder
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
        encoder.start();
        appender.setEncoder(encoder);

        appender.start();

        return appender;
    }
}