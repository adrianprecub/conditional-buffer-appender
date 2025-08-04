package com.mork.cookie.logback.micronaut;

import com.mork.cookie.logback.ConditionalBufferAppender;
import io.micronaut.context.ApplicationContext;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the Micronaut Conditional Logging Configuration.
 */
@MicronautTest(packages = "com.mork.cookie.logback.micronaut")
class ConditionalLoggingConfigurationTest {

    @Inject
    ApplicationContext applicationContext;

    @Test
    void shouldCreateConditionalBufferAppenderBean() {
        // Given & When
        ConditionalBufferAppender appender = applicationContext.getBean(ConditionalBufferAppender.class);

        // Then
        assertThat(appender).isNotNull();
        assertThat(appender.getName()).isEqualTo("CONDITIONAL_BUFFER");
        assertThat(appender.getMaxBufferSize()).isEqualTo(1000);
        assertThat(appender.getBufferTimeoutMinutes()).isEqualTo(10);
        assertThat(appender.getCleanupIntervalMinutes()).isEqualTo(5);
    }

    @Test
    void shouldHaveSingletonScope() {
        // Given & When
        ConditionalBufferAppender appender1 = applicationContext.getBean(ConditionalBufferAppender.class);
        ConditionalBufferAppender appender2 = applicationContext.getBean(ConditionalBufferAppender.class);

        // Then
        assertThat(appender1).isSameAs(appender2);
    }
}