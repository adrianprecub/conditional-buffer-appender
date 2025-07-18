package com.mork.cookie.logback;

import java.util.UUID;

/**
 * Thread-local context for tracking request-scoped logging information.
 * Stores request ID and error state for conditional log display.
 */
public class RequestLoggingContext {
    private static final ThreadLocal<String> REQUEST_ID = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> HAS_ERROR = new ThreadLocal<>();

    /**
     * Sets the request ID for the current thread and initializes error state.
     * 
     * @param requestId the unique request identifier
     */
    public static void setRequestId(String requestId) {
        REQUEST_ID.set(requestId);
        HAS_ERROR.set(false); // Initialize error flag
    }

    /**
     * Gets the request ID for the current thread.
     * 
     * @return the request ID, or null if not set
     */
    public static String getRequestId() {
        return REQUEST_ID.get();
    }

    /**
     * Marks the current request as having an error.
     */
    public static void markError() {
        HAS_ERROR.set(true);
    }

    /**
     * Checks if the current request has an error.
     * 
     * @return true if the request has an error, false otherwise
     */
    public static boolean hasError() {
        Boolean error = HAS_ERROR.get();
        return error != null && error;
    }

    /**
     * Clears the request context for the current thread.
     * Should be called at the end of request processing.
     */
    public static void clear() {
        REQUEST_ID.remove();
        HAS_ERROR.remove();
    }

    /**
     * Generates a new unique request ID.
     * 
     * @return a new UUID-based request ID
     */
    public static String generateRequestId() {
        return UUID.randomUUID().toString();
    }
}