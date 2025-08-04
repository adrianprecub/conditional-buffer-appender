# Conditional Buffer Appender

A smart Logback appender that dynamically displays logs based on request completion status. When a request completes successfully, only INFO-level logs are shown. When errors occur, all logs (DEBUG, INFO, WARN, ERROR) are displayed to aid in debugging.

## Quick Start

### 1. Install to Local Maven Repository

Since this library is not yet published to Maven Central, install it locally first:

```bash
# clone github repo
cd conditional-buffer-appender
mvn clean install
```

### 2. Add Dependency

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>com.mork.cookie</groupId>
    <artifactId>conditional-buffer-appender</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 3. Configure Logback

Add to your `logback-spring.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="CONDITIONAL_BUFFER" class="com.mork.cookie.logback.ConditionalBufferAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
        <maxBufferSize>1000</maxBufferSize>
        <bufferTimeoutMinutes>10</bufferTimeoutMinutes>
        <cleanupIntervalMinutes>5</cleanupIntervalMinutes>
    </appender>

    <root level="DEBUG">
        <appender-ref ref="CONDITIONAL_BUFFER"/>
    </root>
</configuration>
```

### 4a. Enable Spring Boot Auto-Configuration

Add to your main application class or configuration:

```java
@Import(ConditionalLoggingConfiguration.class)
@SpringBootApplication
public class YourApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourApplication.class, args);
    }
}
```

### 4b. Enable Micronaut Auto-Configuration

For Micronaut applications, the configuration is automatically discovered. Just ensure the library is on the classpath and the configuration will be loaded automatically.

### 5a. Use in Spring Boot Code

```java
@RestController
public class YourController {
    private static final ConditionalLogger logger = new ConditionalLogger(YourController.class);

    @GetMapping("/api/example")
    public String example() {
        logger.debug("Starting request processing");
        logger.info("Processing API request");
        
        try {
            // Your business logic
            processBusinessLogic();
            logger.info("Request completed successfully");
            return "Success";
        } catch (Exception e) {
            logger.error("Error occurred: {}", e.getMessage(), e);
            return "Error";
        }
    }
}
```

### 5b. Use in Micronaut Code

```java
@Controller
public class YourController {
    private static final ConditionalLogger logger = new ConditionalLogger(YourController.class);

    @Get("/api/example")
    public String example() {
        logger.debug("Starting request processing");
        logger.info("Processing API request");
        
        try {
            // Your business logic
            processBusinessLogic();
            logger.info("Request completed successfully");
            return "Success";
        } catch (Exception e) {
            logger.error("Error occurred: {}", e.getMessage(), e);
            return "Error";
        }
    }
}
```

## How It Works

1. **Request Start**: Filter generates unique request ID and sets up logging context
2. **During Processing**: All log events are buffered per request
3. **Error Detection**: Any ERROR-level log marks the request as failed
4. **Request End**:
   - **Success**: Only INFO logs are displayed
   - **Error**: All buffered logs are displayed

## Key Classes

- **`ConditionalBufferAppender`**: Main Logback appender that buffers and conditionally displays logs
- **`ConditionalLogger`**: Drop-in replacement for SLF4J Logger with same API
- **`RequestLoggingFilter`**: Servlet filter that manages request lifecycle (Spring Boot)
- **`micronaut.RequestLoggingFilter`**: HTTP filter that manages request lifecycle (Micronaut)
- **`RequestLoggingContext`**: Thread-local context for request ID and error state
- **`spring.ConditionalLoggingConfiguration`**: Spring Boot auto-configuration
- **`micronaut.ConditionalLoggingConfiguration`**: Micronaut auto-configuration

## Configuration Options

| Property | Default | Description |
|----------|---------|-------------|
| `maxBufferSize` | 1000 | Maximum log events per request buffer |
| `bufferTimeoutMinutes` | 10 | Buffer cleanup timeout in minutes |
| `cleanupIntervalMinutes` | 5 | Cleanup task interval in minutes |

## License

Apache License 2.0