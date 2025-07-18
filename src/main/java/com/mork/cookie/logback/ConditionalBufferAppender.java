package com.mork.cookie.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.status.InfoStatus;
import ch.qos.logback.core.status.WarnStatus;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.Map;

/**
 * A Logback appender that buffers log events per request and conditionally displays them
 * based on request outcome. For successful requests, only INFO-level logs are shown.
 * For requests with errors, all log levels are displayed for debugging purposes.
 */
public class ConditionalBufferAppender extends AppenderBase<ILoggingEvent> {

    private Encoder<ILoggingEvent> encoder;
    private final Map<String, RequestLogBuffer> requestBuffers = new ConcurrentHashMap<>();
    private final Object consoleLock = new Object();

    // Configuration properties
    private int maxBufferSize = 1000; // Maximum logs per request
    private int bufferTimeoutMinutes = 10; // Buffer cleanup timeout in minutes
    private int cleanupIntervalMinutes = 5; // Cleanup task interval in minutes

    // Scheduled cleanup
    private ScheduledExecutorService cleanupExecutor;

    /**
     * Inner class to hold buffer with metadata
     */
    public static class RequestLogBuffer {
        private final List<ILoggingEvent> events;
        private final long createdTime;
        private volatile long lastAccessTime;

        public RequestLogBuffer() {
            this.events = new CopyOnWriteArrayList<>();
            this.createdTime = System.currentTimeMillis();
            this.lastAccessTime = createdTime;
        }

        public void addEvent(ILoggingEvent event) {
            events.add(event);
            lastAccessTime = System.currentTimeMillis();
        }

        public List<ILoggingEvent> getEvents() {
            lastAccessTime = System.currentTimeMillis();
            return events;
        }

        public boolean isExpired(long timeoutMs) {
            return (System.currentTimeMillis() - lastAccessTime) > timeoutMs;
        }

        public int size() {
            return events.size();
        }

        public long getCreatedTime() {
            return createdTime;
        }

        public long getLastAccessTime() {
            return lastAccessTime;
        }
    }

    public Map<String, RequestLogBuffer> getRequestBuffers() {
        return requestBuffers;
    }

    @Override
    protected void append(ILoggingEvent event) {
        String requestId = RequestLoggingContext.getRequestId();

        if (requestId == null) {
            // No request context, log normally if it's ERROR level
            if (event.getLevel().isGreaterOrEqual(Level.ERROR)) {
                writeToConsole(event);
            }
            return;
        }

        // Get or create buffer for this request
        RequestLogBuffer buffer = requestBuffers.computeIfAbsent(requestId, k -> new RequestLogBuffer());

        // Check buffer size limit
        if (buffer.size() >= maxBufferSize) {
            // Buffer is full, drop the event and log a warning
            addStatus(new WarnStatus("Buffer full for request " + requestId +
                    ", dropping log event: " + event.getMessage(), this));
            return;
        }

        // Buffer the event for this request
        buffer.addEvent(event);

        // If this is an error, mark the request as having an error but DON'T flush yet
        if (event.getLevel().isGreaterOrEqual(Level.ERROR)) {
            RequestLoggingContext.markError();
        }
    }

    /**
     * Method to be called when request is finished
     */
    public void flushRequestLogsIfError(String requestId) {
        RequestLogBuffer buffer = requestBuffers.remove(requestId);
        if (buffer == null) {
            return;
        }

        List<ILoggingEvent> bufferedEvents = buffer.getEvents();
        
        if (RequestLoggingContext.hasError()) {
            // Error occurred - display ALL logs regardless of level
            synchronized (consoleLock) {
                System.out.println("=== REQUEST COMPLETED WITH ERROR - Flushing " +
                        bufferedEvents.size() + " logs for request: " + requestId + " ===");
                for (ILoggingEvent bufferedEvent : bufferedEvents) {
                    writeToConsole(bufferedEvent);
                }
                System.out.println("=== End of request logs for: " + requestId + " ===");
            }
        } else {
            // No error occurred - only display INFO level logs
            List<ILoggingEvent> infoLogs = bufferedEvents.stream()
                    .filter(event -> event.getLevel().equals(Level.INFO))
                    .toList();
            
            if (!infoLogs.isEmpty()) {
                synchronized (consoleLock) {
                    System.out.println("=== REQUEST COMPLETED SUCCESSFULLY - Showing " +
                            infoLogs.size() + " INFO logs for request: " + requestId + " ===");
                    for (ILoggingEvent infoEvent : infoLogs) {
                        writeToConsole(infoEvent);
                    }
                    System.out.println("=== End of request logs for: " + requestId + " ===");
                }
            }
        }
    }

    private void writeToConsole(ILoggingEvent event) {
        try {
            synchronized (consoleLock) {
                System.out.write(encoder.encode(event));
                System.out.flush();
            }
        } catch (Exception e) {
            addStatus(new ErrorStatus("Failed to write log event", this, e));
        }
    }

    public void cleanupRequest(String requestId) {
        requestBuffers.remove(requestId);
    }

    /**
     * Enhanced cleanup method with time-based logic
     */
    public void cleanupExpiredRequests() {
        long timeoutMs = bufferTimeoutMinutes * 60 * 1000L;
        int removedCount = 0;

        var iterator = requestBuffers.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            RequestLogBuffer buffer = entry.getValue();

            if (buffer.isExpired(timeoutMs)) {
                iterator.remove();
                removedCount++;
            }
        }

        if (removedCount > 0) {
            addStatus(new InfoStatus("Cleanup completed: removed " + removedCount + " expired buffers", this));
        }
    }

    /**
     * Force cleanup of all buffers (emergency cleanup)
     */
    public void forceCleanupAll() {
        int buffersRemoved = requestBuffers.size();
        requestBuffers.clear();

        if (buffersRemoved > 0) {
            addStatus(new InfoStatus("Force cleanup: removed " + buffersRemoved + " buffers", this));
        }
    }

    // Configuration getters and setters
    public int getMaxBufferSize() {
        return maxBufferSize;
    }

    public void setMaxBufferSize(int maxBufferSize) {
        this.maxBufferSize = Math.max(1, maxBufferSize);
    }

    public int getBufferTimeoutMinutes() {
        return bufferTimeoutMinutes;
    }

    public void setBufferTimeoutMinutes(int bufferTimeoutMinutes) {
        this.bufferTimeoutMinutes = Math.max(1, bufferTimeoutMinutes);
    }

    public int getCleanupIntervalMinutes() {
        return cleanupIntervalMinutes;
    }

    public void setCleanupIntervalMinutes(int cleanupIntervalMinutes) {
        this.cleanupIntervalMinutes = Math.max(1, cleanupIntervalMinutes);
    }

    public Encoder<ILoggingEvent> getEncoder() {
        return encoder;
    }

    public void setEncoder(Encoder<ILoggingEvent> encoder) {
        this.encoder = encoder;
    }

    @Override
    public void start() {
        if (encoder == null) {
            addError("No encoder set for the appender named [" + name + "].");
            return;
        }

        // Start the encoder
        encoder.start();

        // Start the cleanup executor
        cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ConditionalBufferAppender-Cleanup");
            t.setDaemon(true);
            return t;
        });

        // Schedule periodic cleanup
        cleanupExecutor.scheduleWithFixedDelay(
                this::cleanupExpiredRequests,
                cleanupIntervalMinutes,
                cleanupIntervalMinutes,
                TimeUnit.MINUTES
        );

        addStatus(new InfoStatus("ConditionalBufferAppender started with maxBufferSize=" +
                maxBufferSize + ", bufferTimeoutMinutes=" + bufferTimeoutMinutes +
                ", cleanupIntervalMinutes=" + cleanupIntervalMinutes, this));

        super.start();
    }

    @Override
    public void stop() {
        // Stop the cleanup executor
        if (cleanupExecutor != null && !cleanupExecutor.isShutdown()) {
            cleanupExecutor.shutdown();
            try {
                if (!cleanupExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    cleanupExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                cleanupExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // Force cleanup all remaining buffers
        forceCleanupAll();

        // Stop the encoder
        if (encoder != null) {
            encoder.stop();
        }

        super.stop();
    }
}