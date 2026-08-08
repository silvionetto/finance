# Spring AI Code Generation Skill for GitHub Copilot CLI

## Overview

You now have a fully functional **Spring AI Code Generator** skill installed as a GitHub Copilot CLI extension. This skill generates production-ready Spring AI code based on the official Spring AI reference and curated community resources.

## Installation Location

```
.github/extensions/spring-ai-code-generator/
├── extension.mjs          # Main extension code (7 tools)
└── README.md              # Complete documentation
```

**Status**: ✅ Extension loaded and ready to use

---

## The 7 Code Generation Tools

### Tool 1: `generate_spring_ai_service`
**Purpose**: Create a ChatClient service class

**Generated Code Example**:
```java
@Service
public class EmailAnalyzerService {
    private final ChatClient chatClient;
    
    public EmailAnalyzerService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }
    
    public String analyze(String userInput) {
        return this.chatClient.prompt()
            .user(userInput)
            .call()
            .content();
    }
}
```

**When to use**: Basic LLM interaction, text analysis, summarization

---

### Tool 2: `generate_spring_ai_controller`
**Purpose**: Create REST endpoints for AI interactions

**Generated Code Example**:
```java
@RestController
@RequestMapping("/api")
public class ChatController {
    private final ChatClient chatClient;
    
    public ChatController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }
    
    @GetMapping("/chat")
    public String chat(String message) {
        return this.chatClient.prompt()
            .user(message)
            .call()
            .content();
    }
}
```

**When to use**: HTTP endpoints for chat, API integration

---

### Tool 3: `generate_multi_chatclient_config`
**Purpose**: Configure multiple ChatClient strategies

**Generated Code Example**:
```java
@Configuration
public class ChatClientConfiguration {
    @Bean
    public ChatClient primaryChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
            .defaultSystemPrompt("You are a helpful assistant.")
            .build();
    }
    
    @Bean("advancedChatClient")
    public ChatClient advancedChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
            .defaultSystemPrompt("You are an expert AI assistant...")
            .build();
    }
    
    @Bean("fastChatClient")
    public ChatClient fastChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
            .defaultSystemPrompt("You are a quick and efficient assistant.")
            .build();
    }
}
```

**When to use**: Multiple models, different strategies per task, fallback mechanisms

---

### Tool 4: `generate_structured_output_service`
**Purpose**: Extract and map structured data from LLM responses

**Generated Code Example**:
```java
@Service
public class TradeExtractorService {
    private final ChatClient chatClient;
    
    public record Trade(String symbol, String action) {}
    
    public Trade extractTrade(String input) {
        return this.chatClient.prompt()
            .user("Extract and structure the following data: " + input)
            .call()
            .entity(Trade.class);
    }
    
    public List<Trade> extractList(String input) {
        return this.chatClient.prompt()
            .user("Extract multiple items from: " + input)
            .call()
            .entity(new ParameterizedTypeReference<List<Trade>>() {});
    }
}
```

**When to use**: Email parsing, data extraction, structured responses

---

### Tool 5: `generate_chatresponse_service`
**Purpose**: Track token usage and metadata

**Generated Code Example**:
```java
@Service
public class TokenUsageService {
    private final ChatClient chatClient;
    
    public ChatResponse getDetailedResponse(String userInput) {
        ChatResponse response = this.chatClient.prompt()
            .user(userInput)
            .call()
            .chatResponse();
        
        int totalTokens = response.getMetadata().getUsage().getTotalTokens();
        logger.info("Tokens used: {}", totalTokens);
        
        return response;
    }
}
```

**When to use**: Cost tracking, observability, token monitoring

---

### Tool 6: `generate_streaming_service`
**Purpose**: Real-time streaming responses

**Generated Code Example**:
```java
@Service
public class RealtimeAiService {
    private final ChatClient chatClient;
    
    public Flux<String> streamResponse(String userInput) {
        return this.chatClient.prompt()
            .user(userInput)
            .stream()
            .content();
    }
}
```

**When to use**: Server-Sent Events, real-time dashboards, long-running operations

---

### Tool 7: `spring_ai_best_practices`
**Purpose**: Reference guide for best practices

**Topics Available**:
- `chatclient` - ChatClient usage and patterns
- `structured-output` - Entity mapping and type-safe extraction
- `multiple-models` - Working with multiple ChatClient instances
- `streaming` - Streaming responses and Flux API
- `configuration` - Spring AI configuration and setup

**Example Output**:
```
Spring AI ChatClient Best Practices:

1. Constructor Injection: Always inject ChatClient.Builder via constructor
2. Fluent API: Use the fluent API (chatClient.prompt().user(...).call().content())
3. System Prompts: Set default system prompts in configuration
4. Error Handling: Wrap ChatClient calls in try-catch
5. Logging: Use SLF4J Logger for debugging
6. Spring Boot Compatibility: Align Spring Boot and Spring AI versions using official compatibility guidance
7. ChatClient is thread-safe: Can be injected as singleton
```

---

## Project Integration

This skill is designed to enhance Spring Boot projects that use Spring AI:

### For Email Processing
```
generate_structured_output_service 
  className=EmailParserService 
  entityName=EmailSummary
```

Generates a service to extract structured summaries from email bodies.

### For Script Review
```
generate_spring_ai_service 
  className=ScriptAnalyzerService 
  methodName=analyzeScript
```

Generates a service to analyze video scripts using LLM.

### For REST APIs
```
generate_spring_ai_controller 
  className=ScriptReviewController
  basePath=/api/scripts
```

Generates HTTP endpoints for script review workflows.

### For Real-time Updates
```
generate_streaming_service 
  className=RealtimeScriptReviewService
```

Generates Server-Sent Events endpoints for real-time feedback.

### For Trade Extraction
```
generate_structured_output_service 
  className=TradeExtractorService 
  entityName=Trade
```

Generates type-safe trade data extraction from email.

---

## Spring AI Key Concepts

### ChatClient - The Main Interface
- Fluent builder API (similar to WebClient)
- Synchronous (`call()`) and streaming (`stream()`) modes
- Configurable system prompts
- Type-safe structured output

### Fluent API Pattern
```java
String response = chatClient.prompt()
    .user("Your question")
    .call()
    .content();
```

### Structured Output (Type-Safe)
```java
User user = chatClient.prompt()
    .user("Extract user info")
    .call()
    .entity(User.class);
```

vs old approach (string parsing):
```java
String response = chatClient.prompt()
    .user("Extract user info")
    .call()
    .content();
// Manual parsing... error-prone!
```

### Streaming for Real-time
```java
Flux<String> stream = chatClient.prompt()
    .user("Long response")
    .stream()
    .content();
```

---

## Configuration Reference

Add to `application.properties`:

```properties
# Ollama (local)
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.model=llama3:8b

# OR OpenAI
spring.ai.openai.api-key=${OPENAI_API_KEY}
spring.ai.openai.chat.options.model=gpt-4

# OR Anthropic
spring.ai.anthropic.api-key=${ANTHROPIC_API_KEY}
spring.ai.anthropic.chat.options.model=claude-3-opus

# Temperature (0.0 = deterministic, 1.0 = creative)
spring.ai.chat.client.default-options.temperature=0.7

# Disable auto-config if managing ChatClients manually
spring.ai.chat.client.enabled=false
```

---

## Required Dependencies

Add to `build.gradle`:

```gradle
dependencies {
    implementation platform("org.springframework.ai:spring-ai-bom:2.0.0")
    
    // Choose your model provider
    implementation "org.springframework.ai:spring-ai-ollama-starter"
    // OR implementation "org.springframework.ai:spring-ai-openai-starter"
    // OR implementation "org.springframework.ai:spring-ai-anthropic-starter"
}
```

---

## Best Practices Summary

✅ **Always use Constructor Injection**
```java
public MyService(ChatClient.Builder builder) {
    this.chatClient = builder.build();
}
```

✅ **Use Fluent API**
```java
chatClient.prompt().user(...).call().content()
```

✅ **Prefer Type-Safe Entity Mapping**
```java
User user = chatClient.prompt(...).call().entity(User.class);
```

✅ **Log with SLF4J**
```java
private static final Logger logger = LoggerFactory.getLogger(...);
logger.info("Token usage: {}", tokens);
```

✅ **Handle Errors Gracefully**
```java
try {
    return chatClient.prompt().user(...).call().content();
} catch (Exception e) {
    logger.error("LLM call failed", e);
    return fallbackResponse;
}
```

❌ **Avoid Manual String Parsing**
```java
// Bad - error prone
String response = chatClient.prompt(...).call().content();
JSONObject json = new JSONObject(response);
User user = new User(json.getString("name"));
```

❌ **Avoid Field Injection**
```java
// Bad - harder to test
@Autowired ChatClient chatClient;
```

---

## Troubleshooting

### Extension Not Showing Up
```
/ clear
```
Then try again. Extensions reload on session clear.

### "Tool not found" Error
Run: `/list tools` and verify `generate_spring_ai_*` tools are present

### Java Version Issue
Ensure Java 21+ is configured. Check:
```bash
./gradlew --version
```

### Compilation Errors
Generated code uses Java 21+ syntax (records, etc.). Ensure project targets Java 21+.

### Spring Boot Version
Required: Spring Boot 4.0.x or 4.1.x. Check:
```gradle
ext {
    set('springBootVersion', '4.0.2')
}
```

---

## Documentation References

📚 **Official Spring AI Documentation**
- Main: https://docs.spring.io/spring-ai/reference/
- Spring AI Project: https://spring.io/projects/spring-ai
- Awesome Spring AI: https://github.com/spring-ai-community/awesome-spring-ai

---

## Summary

You now have a powerful code generation tool that creates production-ready Spring AI code. All generated code:

✅ Follows Spring best practices
✅ Uses constructor injection
✅ Integrates SLF4J logging
✅ Supports type-safe structured outputs
✅ Includes streaming capabilities
✅ Is compatible with Spring Boot + Spring AI project conventions
✅ Is based on official Spring AI documentation and curated community resources

---

**Last Updated**: 2026
**Spring AI**: Reference-aligned (current)
**Spring Boot**: 4.0+
**Java**: 21+
