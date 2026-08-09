# Copilot Instructions for Spring Boot + Spring AI Finance Project

## Project Overview
This is a Spring Boot 4.1.1-SNAPSHOT application with Spring AI OpenAI integration. It provides:
- A chat UI served via Thymeleaf at `/`
- A JSON chat API at `POST /api/chat` with conversation memory
- Actuator endpoints for monitoring
- Tool integration (DateTimeTools) for AI function calling

## Java & Spring AI API Patterns

### ChatClient Message Handling
When adding conversation history to ChatClient, use the `.messages()` API with a list of Message objects:
```java
// ✅ CORRECT - for context/history
List<Message> messages = chatMemory.getMessages();
String response = chatClient.prompt()
    .messages(messages)  // Pass full conversation history
    .tools(dateTimeTools)
    .call()
    .content();

// ❌ AVOID - .user() doesn't support full message history
chatClient.prompt()
    .user(prompt)  // Only for single user prompt
    .call()
    .content();
```

### Message Interface API
Always use `.getText()` to access message content, not `.getContent()`:
```java
// ✅ CORRECT
String content = message.getText();

// ❌ WRONG - Method doesn't exist in Spring AI 2.0.0
String content = message.getContent();
```

### Component Registration & Bean Conflicts
- Use `@Component` for simple singleton services that need dependency injection
- Use `@Configuration` + `@Bean` only when you need conditional bean creation or setup logic
- **Bean Override Issue**: When Spring tries to register the same bean twice, you'll get:
  ```
  BeanDefinitionOverrideException: Invalid bean definition with name...
  ```
  **Solution**: Ensure each bean is registered exactly once. If using both `@Component` and `@Bean` for the same class, remove one.

## Building & Testing Best Practices

### Gradle Build Commands
```bash
# Build only (skip tests) - useful for checking compilation
./gradlew build -x test

# Build and run all tests
./gradlew build

# Run specific test class
./gradlew test --tests com.silvionetto.finance.ChatMemoryTest

# Run specific test method
./gradlew test --tests com.silvionetto.finance.ChatMemoryTest.shouldStoreUserMessage
```

### Test Organization
- **Unit tests** (no @SpringBootTest): Test business logic in isolation. Faster and no bean conflicts.
- **Integration tests** (@SpringBootTest): Test full app context. Use sparingly, mock external dependencies.
- Example: ChatMemoryTest is a unit test (no Spring context), ChatControllerTests uses MockMvc.

## Message History & Conversation Context

### Storing Conversation Memory
When implementing stateful chat:
1. Add user message to memory **before** calling ChatClient
2. Pass full message history to ChatClient via `.messages()`
3. Add assistant response to memory **after** receiving it

```java
public String chat(String prompt) {
    chatMemory.addUserMessage(prompt);  // Store user input first
    
    List<Message> messages = chatMemory.getMessages();
    String response = chatClient.prompt()
        .messages(messages)
        .tools(this.dateTimeTools)
        .call()
        .content();
    
    chatMemory.addAssistantMessage(response);  // Store response after
    return response;
}
```

### Memory Limits
Consider setting a max message limit to prevent unbounded growth:
```java
public class InMemoryChatMemory {
    private final int maxMessages = 100;  // Configurable window
    
    private void addMessage(Message message) {
        messages.add(message);
        if (messages.size() > maxMessages) {
            messages.remove(0);  // FIFO: remove oldest when full
        }
    }
}
```

## API Endpoint Patterns

### Chat Endpoint with Memory
```java
@PostMapping
public ChatResponse chat(@RequestBody ChatRequest request) {
    return new ChatResponse(this.chatService.chat(request.prompt()));
}

// Add endpoint to reset memory
@DeleteMapping("/memory")
public void resetMemory() {
    this.chatService.clearMemory();
}
```

### Request/Response Records
Use records for simple DTOs:
```java
public record ChatRequest(String prompt) {}
public record ChatResponse(String response) {}
```

## Dependency Versions & Configuration

### Spring AI 2.0.0 Notes
- Imported via BOM in `build.gradle`:
  ```gradle
  ext {
      set('springAiVersion', "2.0.0")
  }
  dependencyManagement {
      imports {
          mavenBom "org.springframework.ai:spring-ai-bom:${springAiVersion}"
      }
  }
  ```
- Message interface uses `.getText()` (not `.getContent()`)
- ChatClient uses `.messages(List<Message>)` for context history
- Tool integration via `@Tool` annotation on @Component or @Bean methods

### Spring Boot 4.1.1-SNAPSHOT
- Requires Java 25 toolchain (verified in `build.gradle`)
- Snapshot repo: `https://repo.spring.io/snapshot`
- Build with `./gradlew.bat build`

## Configuration Files

### application.properties
```properties
spring.application.name=finance
spring.ai.openai.api-key=${OPENAI_API_KEY:${AZURE_OPENAI_API_KEY:}}
spring.ai.openai.base-url=${OPENAI_BASE_URL:https://api.openai.com/v1}
spring.ai.openai.chat.options.model=${OPENAI_MODEL:gpt-4o-mini}
```

Supports environment variable overrides:
- `OPENAI_API_KEY` - Primary API key
- `AZURE_OPENAI_API_KEY` - Fallback for Azure OpenAI
- `OPENAI_BASE_URL` - Custom API endpoint
- `OPENAI_MODEL` - Model selection (default: gpt-4o-mini)

## Common Pitfalls & Solutions

| Issue | Cause | Solution |
|-------|-------|----------|
| `BeanDefinitionOverrideException` | Bean registered twice | Remove duplicate @Component/@Bean for same class |
| `symbol: method getContent()` | Using wrong Message API | Use `.getText()` instead |
| `.user()` not passing context | ChatClient.user() ignores history | Use `.messages(List)` with full history |
| Tests fail to load Spring context | Bean autowiring issues | Make unit tests standalone (no @SpringBootTest) |
| Message history missing in AI responses | Not adding messages to memory first | Add to memory BEFORE calling ChatClient |

## Next Steps & Future Improvements

- [ ] Add persistent storage (database) for conversation history
- [ ] Implement conversation IDs for multi-session support
- [ ] Add token counting to manage context window limits
- [ ] Implement conversation search/filtering
- [ ] Add message editing and deletion endpoints
- [ ] Consider async ChatClient calls for better performance

---

**Last Updated**: 2026-08-09  
**Tested With**: Spring Boot 4.1.1-SNAPSHOT, Spring AI 2.0.0, Java 25, Gradle 9.5.1
