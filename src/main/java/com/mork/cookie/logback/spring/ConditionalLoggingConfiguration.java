package com.mork.cookie.logback.spring;

import com.mork.cookie.logback.ConditionalBufferAppender;
import com.mork.cookie.logback.RequestLoggingFilter;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot configuration for the Conditional Buffer Appender.
 * This configuration class sets up the necessary beans for request-scoped
 * conditional logging in a Spring Boot application.
 */
@Configuration
public class ConditionalLoggingConfiguration {

    /**
     * Creates and configures the ConditionalBufferAppender bean.
     * First tries to get an existing appender from logback configuration,
     * then falls back to programmatic creation if not found.
     * 
     * @return the configured ConditionalBufferAppender
     */
    @Bean
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

    /**
     * Registers the RequestLoggingFilter to intercept HTTP requests
     * and manage request-scoped logging context.
     * 
     * @param appender the ConditionalBufferAppender to use
     * @return the configured filter registration bean
     */
    @Bean
    public FilterRegistrationBean<RequestLoggingFilter> requestLoggingFilter(ConditionalBufferAppender appender) {
        FilterRegistrationBean<RequestLoggingFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new RequestLoggingFilter(appender));
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(1);
        return registrationBean;
    }
}