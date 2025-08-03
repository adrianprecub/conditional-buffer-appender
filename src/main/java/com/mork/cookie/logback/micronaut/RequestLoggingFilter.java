package com.mork.cookie.logback.micronaut;

import com.mork.cookie.logback.ConditionalBufferAppender;
import com.mork.cookie.logback.RequestLoggingContext;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import jakarta.inject.Inject;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * Micronaut HTTP filter that manages request-scoped logging context.
 * Sets up request ID at the beginning of request processing and
 * triggers conditional log flushing at the end.
 */
@Filter("/**")
public class RequestLoggingFilter implements HttpServerFilter {

    private final ConditionalBufferAppender appender;

    /**
     * Creates a new RequestLoggingFilter with the specified appender.
     * 
     * @param appender the ConditionalBufferAppender to use for log flushing
     */
    @Inject
    public RequestLoggingFilter(ConditionalBufferAppender appender) {
        this.appender = appender;
    }

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        String requestId = RequestLoggingContext.generateRequestId();
        RequestLoggingContext.setRequestId(requestId);

        return Mono.from(chain.proceed(request))
                .doFinally(signal -> {
                    // After request is completely finished, check if there was an error and flush if needed
                    if (appender != null) {
                        try {
                            appender.flushRequestLogsIfError(requestId);
                        } catch (Exception e) {
                            // Log the exception but don't propagate it
                            System.err.println("Error flushing request logs: " + e.getMessage());
                        }
                    }

                    // Clean up the request context
                    RequestLoggingContext.clear();
                });
    }
}