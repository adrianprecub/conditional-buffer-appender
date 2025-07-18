package com.mork.cookie.logback;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestLoggingContextTest {

    @BeforeEach
    void setUp() {
        // Clear any existing context before each test
        RequestLoggingContext.clear();
    }

    @AfterEach
    void tearDown() {
        // Clear context after each test to prevent interference
        RequestLoggingContext.clear();
    }

    @Test
    void testSetAndGetRequestId() {
        String requestId = "test-request-123";
        
        // Initially should be null
        assertNull(RequestLoggingContext.getRequestId());
        
        // Set request ID
        RequestLoggingContext.setRequestId(requestId);
        
        // Should return the set request ID
        assertThat(RequestLoggingContext.getRequestId()).isEqualTo(requestId);
    }

    @Test
    void testSetRequestIdInitializesErrorState() {
        String requestId = "test-request-456";
        
        // Initially should have no error
        assertFalse(RequestLoggingContext.hasError());
        
        // Set request ID
        RequestLoggingContext.setRequestId(requestId);
        
        // Error state should be initialized to false
        assertFalse(RequestLoggingContext.hasError());
    }

    @Test
    void testMarkError() {
        String requestId = "test-request-789";
        RequestLoggingContext.setRequestId(requestId);
        
        // Initially should have no error
        assertFalse(RequestLoggingContext.hasError());
        
        // Mark error
        RequestLoggingContext.markError();
        
        // Should now have error
        assertTrue(RequestLoggingContext.hasError());
    }

    @Test
    void testMarkErrorWithoutRequestId() {
        // Mark error without setting request ID
        RequestLoggingContext.markError();
        
        // Should still work (though request ID is null)
        assertTrue(RequestLoggingContext.hasError());
    }

    @Test
    void testHasErrorWhenNotSet() {
        // Without setting anything, should return false
        assertFalse(RequestLoggingContext.hasError());
    }

    @Test
    void testClear() {
        String requestId = "test-request-clear";
        RequestLoggingContext.setRequestId(requestId);
        RequestLoggingContext.markError();
        
        // Verify values are set
        assertThat(RequestLoggingContext.getRequestId()).isEqualTo(requestId);
        assertTrue(RequestLoggingContext.hasError());
        
        // Clear context
        RequestLoggingContext.clear();
        
        // Should be cleared
        assertNull(RequestLoggingContext.getRequestId());
        assertFalse(RequestLoggingContext.hasError());
    }

    @Test
    void testGenerateRequestId() {
        String requestId1 = RequestLoggingContext.generateRequestId();
        String requestId2 = RequestLoggingContext.generateRequestId();
        
        // Should generate valid UUIDs
        assertNotNull(requestId1);
        assertNotNull(requestId2);
        
        // Should be different
        assertThat(requestId1).isNotEqualTo(requestId2);
        
        // Should be valid UUID format (36 characters with dashes)
        assertThat(requestId1).hasSize(36);
        assertThat(requestId2).hasSize(36);
        assertThat(requestId1).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        assertThat(requestId2).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    void testThreadLocalIsolation() throws InterruptedException, ExecutionException {
        String mainThreadRequestId = "main-thread-request";
        RequestLoggingContext.setRequestId(mainThreadRequestId);
        RequestLoggingContext.markError();
        
        // Verify main thread state
        assertThat(RequestLoggingContext.getRequestId()).isEqualTo(mainThreadRequestId);
        assertTrue(RequestLoggingContext.hasError());
        
        // Test in another thread
        CompletableFuture<Void> otherThreadTest = CompletableFuture.runAsync(() -> {
            // Other thread should have no context initially
            assertNull(RequestLoggingContext.getRequestId());
            assertFalse(RequestLoggingContext.hasError());
            
            // Set context in other thread
            String otherThreadRequestId = "other-thread-request";
            RequestLoggingContext.setRequestId(otherThreadRequestId);
            
            // Should be isolated from main thread
            assertThat(RequestLoggingContext.getRequestId()).isEqualTo(otherThreadRequestId);
            assertFalse(RequestLoggingContext.hasError());
        });
        
        otherThreadTest.get(); // Wait for completion
        
        // Main thread context should remain unchanged
        assertThat(RequestLoggingContext.getRequestId()).isEqualTo(mainThreadRequestId);
        assertTrue(RequestLoggingContext.hasError());
    }

    @Test
    void testMultipleThreadsWithDifferentContexts() throws InterruptedException, ExecutionException, TimeoutException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        try {
            // Create futures for each thread
            Set<Future<String>> futures = new HashSet<>();
            
            for (int i = 0; i < threadCount; i++) {
                final int threadNum = i;
                Future<String> future = executor.submit(() -> {
                    String requestId = "thread-" + threadNum + "-request";
                    RequestLoggingContext.setRequestId(requestId);
                    
                    // Mark error for even numbered threads
                    if (threadNum % 2 == 0) {
                        RequestLoggingContext.markError();
                    }
                    
                    // Verify context is correct for this thread
                    assertThat(RequestLoggingContext.getRequestId()).isEqualTo(requestId);
                    
                    if (threadNum % 2 == 0) {
                        assertTrue(RequestLoggingContext.hasError());
                    } else {
                        assertFalse(RequestLoggingContext.hasError());
                    }
                    
                    return requestId;
                });
                
                futures.add(future);
            }
            
            // Wait for all threads to complete and verify results
            Set<String> requestIds = new HashSet<>();
            for (Future<String> future : futures) {
                String requestId = future.get(5, TimeUnit.SECONDS);
                requestIds.add(requestId);
            }
            
            // Should have unique request IDs for each thread
            assertThat(requestIds).hasSize(threadCount);
            
        } finally {
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    @Test
    void testRequestIdOverwrite() {
        String firstRequestId = "first-request";
        String secondRequestId = "second-request";
        
        // Set first request ID
        RequestLoggingContext.setRequestId(firstRequestId);
        assertThat(RequestLoggingContext.getRequestId()).isEqualTo(firstRequestId);
        assertFalse(RequestLoggingContext.hasError());
        
        // Mark error
        RequestLoggingContext.markError();
        assertTrue(RequestLoggingContext.hasError());
        
        // Set second request ID (should reset error state)
        RequestLoggingContext.setRequestId(secondRequestId);
        assertThat(RequestLoggingContext.getRequestId()).isEqualTo(secondRequestId);
        assertFalse(RequestLoggingContext.hasError()); // Should be reset
    }

    @Test
    void testClearOnlyRemovesCurrentThread() throws InterruptedException, ExecutionException {
        String mainThreadRequestId = "main-request";
        RequestLoggingContext.setRequestId(mainThreadRequestId);
        RequestLoggingContext.markError();
        
        // Create another thread with its own context
        CompletableFuture<Void> otherThreadTest = CompletableFuture.runAsync(() -> {
            String otherRequestId = "other-request";
            RequestLoggingContext.setRequestId(otherRequestId);
            
            // Clear only affects current thread
            RequestLoggingContext.clear();
            
            // This thread should be cleared
            assertNull(RequestLoggingContext.getRequestId());
            assertFalse(RequestLoggingContext.hasError());
        });
        
        otherThreadTest.get();
        
        // Main thread should still have its context
        assertThat(RequestLoggingContext.getRequestId()).isEqualTo(mainThreadRequestId);
        assertTrue(RequestLoggingContext.hasError());
    }

    @Test
    void testNullRequestId() {
        // Test setting null request ID
        RequestLoggingContext.setRequestId(null);
        
        assertNull(RequestLoggingContext.getRequestId());
        assertFalse(RequestLoggingContext.hasError());
    }

    @Test
    void testEmptyRequestId() {
        // Test setting empty request ID
        RequestLoggingContext.setRequestId("");
        
        assertThat(RequestLoggingContext.getRequestId()).isEqualTo("");
        assertFalse(RequestLoggingContext.hasError());
    }

    @Test
    void testMultipleErrorMarkings() {
        String requestId = "test-multiple-errors";
        RequestLoggingContext.setRequestId(requestId);
        
        // Mark error multiple times
        RequestLoggingContext.markError();
        assertTrue(RequestLoggingContext.hasError());
        
        RequestLoggingContext.markError();
        assertTrue(RequestLoggingContext.hasError());
        
        RequestLoggingContext.markError();
        assertTrue(RequestLoggingContext.hasError());
        
        // Should still be in error state
        assertTrue(RequestLoggingContext.hasError());
    }

    @Test
    void testGenerateRequestIdUniqueness() {
        Set<String> generatedIds = new HashSet<>();
        int numberOfIds = 1000;
        
        // Generate many IDs
        for (int i = 0; i < numberOfIds; i++) {
            String requestId = RequestLoggingContext.generateRequestId();
            generatedIds.add(requestId);
        }
        
        // All should be unique
        assertThat(generatedIds).hasSize(numberOfIds);
    }
}