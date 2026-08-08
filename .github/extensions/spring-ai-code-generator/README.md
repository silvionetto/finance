# Spring AI Code Generator Extension

A GitHub Copilot CLI extension that generates production-ready Spring AI code for Spring Boot 4.0+ applications. This extension scaffolds ChatClient services, controllers, configurations, and best-practice guidance aligned with the current Spring AI reference and the Awesome Spring AI community curation.

## Documentation Reference

- **Spring AI Official Docs**: https://docs.spring.io/spring-ai/reference/
- **Awesome Spring AI**: https://github.com/spring-ai-community/awesome-spring-ai
- **Version Guidance**: Follow the Spring AI/Spring Boot compatibility matrix from official docs
- **Spring Boot**: 4.0.x or 4.1.x
- **Java**: 21+ recommended

## Available Tools

### 1. `generate_spring_ai_service`

Generates a Spring AI ChatClient service class with dependency injection and logging.

**Parameters:**
- `className` (required): Service class name (e.g., `TextAnalysisService`)
- `methodName` (required): Main service method name (e.g., `analyze`)
- `description` (optional): What the service does

**Example Usage:**
```
generate_spring_ai_service className=SentimentAnalysisService methodName=analyzeSentiment description=Analyzes text sentiment using LLM
```

**Generated Pattern:**
- Constructor injection of `ChatClient.Builder`
- Fluent ChatClient API usage
- SLF4J logging integration
- Clean, production-ready code

---

### 2. `generate_spring_ai_controller`

Generates a Spring Boot REST controller with ChatClient integration for HTTP endpoints.

**Parameters:**
- `className` (required): Controller class name (e.g., `ChatController`)
- `basePath` (optional): REST endpoint base path (e.g., `/api/chat`)

**Example Usage:**
```
generate_spring_ai_controller className=AiController basePath=/api/ai
```

**Generated Pattern:**
- `@RestController` annotation
- Constructor-based dependency injection
- GET endpoint for chat interactions
- Fluent ChatClient API

---

### 3. `generate_multi_chatclient_config`

Generates a Spring `@Configuration` class defining multiple ChatClient beans for different strategies (primary, advanced, fast).

**Parameters:**
- `className` (required): Configuration class name (e.g., `ChatClientConfig`)

**Example Usage:**
```
generate_multi_chatclient_config className=AiConfiguration
```

**Generated Pattern:**
- Three pre-configured ChatClient beans:
  - **Primary**: Default helpful assistant
  - **Advanced**: Expert AI with deep knowledge
  - **Fast**: Quick and efficient responses
- Use `@Qualifier` to inject specific client
- Different system prompts per client

---

### 4. `generate_structured_output_service`

Generates a service that extracts structured data from LLM responses using type-safe entity mapping.

**Parameters:**
- `className` (required): Service class name (e.g., `TradeExtractorService`)
- `entityName` (required): Entity class name (e.g., `Trade`)

**Example Usage:**
```
generate_structured_output_service className=EmailParserService entityName=EmailSummary
```

**Generated Pattern:**
- Single object extraction with `entity(Class.class)`
- List extraction with `ParameterizedTypeReference<List<T>>`
- Type-safe structured outputs
- No string parsing needed

---

### 5. `generate_chatresponse_service`

Generates a service that captures ChatResponse metadata for observability (token usage, cost tracking).

**Parameters:**
- `className` (required): Service class name (e.g., `AnalyticsService`)

**Example Usage:**
```
generate_chatresponse_service className=TokenUsageTracker
```

**Generated Pattern:**
- Access to ChatResponse object
- Token usage metrics
- Metadata logging
- Cost tracking capabilities

---

### 6. `generate_streaming_service`

Generates a service that streams AI responses using Project Reactor's Flux API for real-time delivery.

**Parameters:**
- `className` (required): Service class name (e.g., `StreamingService`)

**Example Usage:**
```
generate_streaming_service className=RealtimeAiService
```

**Generated Pattern:**
- `stream()` method for streaming
- Returns `Flux<String>` for reactive responses
- Server-Sent Events (SSE) compatible
- Non-blocking response delivery

---

### 7. `spring_ai_best_practices`

Returns detailed best practices, patterns, and curated resources for current Spring AI development.

**Parameters:**
- `topic` (required): One of:
  - `chatclient` - ChatClient usage patterns
  - `structured-output` - Entity mapping and structured data extraction
  - `multiple-models` - Working with multiple ChatClient instances
  - `streaming` - Streaming responses with Flux
  - `configuration` - Configuration and setup
  - `resources` - Curated links from Spring and awesome-spring-ai

**Example Usage:**
```
spring_ai_best_practices topic=chatclient
spring_ai_best_practices topic=structured-output
spring_ai_best_practices topic=resources
```

---

## Quick Start Examples

### Example 1: Simple Chat Service
```
generate_spring_ai_service className=ChatService methodName=chat
```

### Example 2: REST API Endpoint
```
generate_spring_ai_controller className=ChatController
```

### Example 3: Email Extraction with Structured Output
```
generate_structured_output_service className=EmailParserService entityName=EmailSummary
```

### Example 4: Multiple Model Strategy
```
generate_multi_chatclient_config className=AiModelConfiguration
```

### Example 5: Real-time Streaming
```
generate_streaming_service className=RealTimeAiService
```

## Integration

This extension is designed for Spring Boot applications that use Spring AI. Use it to:

1. **Generate Agent Services**: Create ChatClient-based services for AI agents
2. **Build REST APIs**: Scaffold controllers for email processing and analysis
3. **Structure Data Extraction**: Generate services for parsing emails, scripts, and trades
4. **Enable Streaming**: Build real-time response endpoints for video script reviews
5. **Monitor Usage**: Track token consumption and costs

## Spring AI Key Concepts

### ChatClient
The primary interface for communicating with AI models. It provides:
- Fluent builder API similar to WebClient
- Both synchronous (`call()`) and streaming (`stream()`) modes
- System prompts and prompt configuration
- Structured output mapping with `entity()`
- Token usage metadata

### Structured Outputs
Map LLM responses directly to Java types:
```java
// Single object
Entity result = chatClient.prompt()
    .user("Extract data")
    .call()
    .entity(Entity.class);

// List of objects
List<Entity> results = chatClient.prompt()
    .user("Extract multiple items")
    .call()
    .entity(new ParameterizedTypeReference<List<Entity>>() {});
```

### Multiple ChatClients
Different ChatClients for different strategies:
```java
@Autowired
@Qualifier("advancedChatClient")
ChatClient advancedClient;
```

### Streaming
Non-blocking response delivery for real-time applications:
```java
Flux<String> stream = chatClient.prompt()
    .user(input)
    .stream()
    .content();
```

## Best Practices Highlighted

✅ **Constructor Injection**: Always use constructor injection for ChatClient.Builder
✅ **Fluent API**: Leverage the chainable ChatClient API
✅ **Type Safety**: Use entity() for structured outputs instead of string parsing
✅ **Logging**: Integrate SLF4J for observability
✅ **Multiple Models**: Separate configuration for different model strategies
✅ **Error Handling**: Wrap ChatClient calls in proper exception handling
✅ **Streaming**: Use Flux for real-time, non-blocking responses

## Configuration Example

Add to `application.properties`:

```properties
# Spring AI Model Configuration
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.model=llama3:8b
spring.ai.chat.client.default-options.temperature=0.7
spring.ai.chat.client.enabled=false

# Or for OpenAI
spring.ai.openai.api-key=${OPENAI_API_KEY}
spring.ai.openai.chat.options.model=gpt-4
```

## Dependencies Required

```gradle
// In build.gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-webmvc:4.0.2'
    implementation platform('org.springframework.ai:spring-ai-bom:2.0.0')
    implementation 'org.springframework.ai:spring-ai-ollama-starter'
    // OR implementation 'org.springframework.ai:spring-ai-starter-model-openai'
}
```

## Troubleshooting

### Tool Not Available
Ensure extensions are reloaded: `/clear` or `Ctrl+K, Ctrl+T`

### Wrong Spring Boot Version
This extension requires a Spring Boot and Spring AI combination that is officially compatible.

### Missing ChatModel Bean
Ensure you have Spring AI starter configured in `build.gradle` (e.g., `spring-ai-ollama-starter`)

### Compilation Errors
Generated code uses Java 21+ syntax (records, sealed classes). Ensure Java 21+ is configured in your project.

## Contributing

When contributing, keep these conventions:
- Spring Boot 4.0+ compatibility
- Constructor injection pattern
- SLF4J for logging
- Keep generated examples concise and production-oriented

## License

Same as this repository.

---

**Last Updated**: 2026
**Spring AI Version**: Current stable/reference-aligned
**Spring Boot Version**: 4.0+
