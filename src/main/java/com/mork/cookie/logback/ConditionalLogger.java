package com.mork.cookie.logback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A wrapper around SLF4J Logger that provides the same logging interface
 * but works seamlessly with the ConditionalBufferAppender for request-scoped
 * conditional log display.
 */
public class ConditionalLogger {
    private final Logger logger;

    /**
     * Creates a new ConditionalLogger for the specified class.
     * 
     * @param clazz the class to create a logger for
     */
    public ConditionalLogger(Class<?> clazz) {
        this.logger = LoggerFactory.getLogger(clazz);
    }

    /**
     * Creates a new ConditionalLogger with the specified name.
     * 
     * @param name the logger name
     */
    public ConditionalLogger(String name) {
        this.logger = LoggerFactory.getLogger(name);
    }

    /**
     * Logs a debug message.
     * 
     * @param message the message to log
     */
    public void debug(String message) {
        logger.debug(message);
    }

    /**
     * Logs a debug message with parameters.
     * 
     * @param message the message to log
     * @param args the arguments to substitute in the message
     */
    public void debug(String message, Object... args) {
        logger.debug(message, args);
    }

    /**
     * Logs an info message.
     * 
     * @param message the message to log
     */
    public void info(String message) {
        logger.info(message);
    }

    /**
     * Logs an info message with parameters.
     * 
     * @param message the message to log
     * @param args the arguments to substitute in the message
     */
    public void info(String message, Object... args) {
        logger.info(message, args);
    }

    /**
     * Logs a warning message.
     * 
     * @param message the message to log
     */
    public void warn(String message) {
        logger.warn(message);
    }

    /**
     * Logs a warning message with parameters.
     * 
     * @param message the message to log
     * @param args the arguments to substitute in the message
     */
    public void warn(String message, Object... args) {
        logger.warn(message, args);
    }

    /**
     * Logs an error message.
     * 
     * @param message the message to log
     */
    public void error(String message) {
        logger.error(message);
    }

    /**
     * Logs an error message with parameters.
     * 
     * @param message the message to log
     * @param args the arguments to substitute in the message
     */
    public void error(String message, Object... args) {
        logger.error(message, args);
    }

    /**
     * Logs an error message with an exception.
     * 
     * @param message the message to log
     * @param throwable the exception to log
     */
    public void error(String message, Throwable throwable) {
        logger.error(message, throwable);
    }

    /**
     * Gets the underlying SLF4J logger.
     * 
     * @return the underlying logger
     */
    public Logger getLogger() {
        return logger;
    }
}