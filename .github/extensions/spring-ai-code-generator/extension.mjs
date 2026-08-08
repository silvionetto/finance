/**
 * Spring AI Code Generator Extension
 *
 * Generates Spring Boot 4.0+ compatible Spring AI code for:
 * - ChatClient services with structured output
 * - REST controllers using ChatClient
 * - Configuration classes with multiple ChatClient instances
 * - ChatClient builders for different AI model providers
 *
 * References:
 * - Spring AI Reference: https://docs.spring.io/spring-ai/reference/
 * - Awesome Spring AI: https://github.com/spring-ai-community/awesome-spring-ai
 */

import { joinSession } from "@github/copilot-sdk/extension";

const SPRING_AI_PATTERNS = {
  chatClientService: `import org.springframework.ai.chat.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class {{className}} {
    
    private static final Logger logger = LoggerFactory.getLogger({{className}}.class);
    
    private final ChatClient chatClient;
    
    @Autowired
    public {{className}}(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }
    
    public String {{methodName}}(String userInput) {
        return this.chatClient.prompt()
            .user(userInput)
            .call()
            .content();
    }
}`,

  chatClientController: `import org.springframework.ai.chat.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class {{className}} {
    
    private final ChatClient chatClient;
    
    public {{className}}(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }
    
    @GetMapping("/chat")
    public String chat(String message) {
        return this.chatClient.prompt()
            .user(message)
            .call()
            .content();
    }
}`,

  multiChatClientConfig: `import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration
public class {{className}} {
    
    @Bean
    public ChatClient primaryChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
            .defaultSystemPrompt("You are a helpful assistant.")
            .build();
    }
    
    @Bean("advancedChatClient")
    public ChatClient advancedChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
            .defaultSystemPrompt("You are an expert AI assistant with deep knowledge across all domains.")
            .build();
    }
    
    @Bean("fastChatClient")
    public ChatClient fastChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
            .defaultSystemPrompt("You are a quick and efficient assistant.")
            .build();
    }
}`,

  structuredOutputService: `import org.springframework.ai.chat.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.ai.chat.model.ChatResponse;
import java.util.List;

@Service
public class {{className}} {
    
    private final ChatClient chatClient;
    
    public {{className}}(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }
    
    // Example record - replace with your actual entity
    public record {{entityName}}(String property1, String property2) {}
    
    public {{entityName}} extract{{entityName}}(String input) {
        return this.chatClient.prompt()
            .user("Extract and structure the following data: " + input)
            .call()
            .entity({{entityName}}.class);
    }
    
    public List<{{entityName}}> extractList(String input) {
        return this.chatClient.prompt()
            .user("Extract multiple items from: " + input)
            .call()
            .entity(new org.springframework.core.ParameterizedTypeReference<List<{{entityName}}>>() {});
    }
}`,

  chatResponseService: `import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class {{className}} {
    
    private static final Logger logger = LoggerFactory.getLogger({{className}}.class);
    
    private final ChatClient chatClient;
    
    public {{className}}(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }
    
    public ChatResponse getDetailedResponse(String userInput) {
        ChatResponse response = this.chatClient.prompt()
            .user(userInput)
            .call()
            .chatResponse();
        
        // Access metadata
        int totalTokens = response.getMetadata().getUsage().getTotalTokens();
        logger.info("Tokens used: {}", totalTokens);
        
        return response;
    }
}`,

  streamingService: `import org.springframework.ai.chat.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class {{className}} {
    
    private final ChatClient chatClient;
    
    public {{className}}(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }
    
    public Flux<String> streamResponse(String userInput) {
        return this.chatClient.prompt()
            .user(userInput)
            .stream()
            .content();
    }
}`,

  advisorsService: `import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class {{className}} {
    
    private final ChatClient chatClient;
    
    public {{className}}(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }
    
    public String generateWithAdvisor(String userInput) {
        // Enable native structured output if model supports it
        return this.chatClient.prompt()
            .advisors(org.springframework.ai.chat.client.AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
            .user(userInput)
            .call()
            .content();
    }
}`,
};

const session = await joinSession({
    tools: [
        {
            name: "generate_spring_ai_service",
            description: "Generate a Spring AI ChatClient service class with current Spring AI best practices",
            parameters: {
                type: "object",
                properties: {
                    className: {
                        type: "string",
                        description: "The name of the service class (e.g., AnalysisService, SummarizationService)"
                    },
                    methodName: {
                        type: "string",
                        description: "The name of the main service method (e.g., analyze, summarize)"
                    },
                    description: {
                        type: "string",
                        description: "Brief description of what the service does"
                    }
                },
                required: ["className", "methodName"]
            },
            handler: async (args) => {
                const code = SPRING_AI_PATTERNS.chatClientService
                    .replace(/{{className}}/g, args.className)
                    .replace(/{{methodName}}/g, args.methodName);
                
                return {
                    textResultForLlm: `Generated Spring AI Service:\n\n\`\`\`java\n${code}\n\`\`\`\n\nKey features:\n- Injects ChatClient.Builder via constructor\n- Uses fluent ChatClient API\n- Returns String response\n- Includes logging`,
                    resultType: "success"
                };
            }
        },
        {
            name: "generate_spring_ai_controller",
            description: "Generate a Spring Boot REST controller using Spring AI ChatClient",
            parameters: {
                type: "object",
                properties: {
                    className: {
                        type: "string",
                        description: "The name of the controller class (e.g., ChatController, AiController)"
                    },
                    basePath: {
                        type: "string",
                        description: "The base path for REST endpoints (e.g., /api/chat)"
                    }
                },
                required: ["className"]
            },
            handler: async (args) => {
                const code = SPRING_AI_PATTERNS.chatClientController
                    .replace(/{{className}}/g, args.className);
                
                return {
                    textResultForLlm: `Generated Spring Boot REST Controller:\n\n\`\`\`java\n${code}\n\`\`\`\n\nKey features:\n- @RestController with ChatClient\n- Constructor injection of ChatClient.Builder\n- GET endpoint for chat interactions\n- Fluent ChatClient API usage`,
                    resultType: "success"
                };
            }
        },
        {
            name: "generate_multi_chatclient_config",
            description: "Generate a Spring configuration class with multiple ChatClient beans for different use cases (fast, advanced, default)",
            parameters: {
                type: "object",
                properties: {
                    className: {
                        type: "string",
                        description: "The name of the configuration class (e.g., ChatClientConfiguration)"
                    }
                },
                required: ["className"]
            },
            handler: async (args) => {
                const code = SPRING_AI_PATTERNS.multiChatClientConfig
                    .replace(/{{className}}/g, args.className);
                
                return {
                    textResultForLlm: `Generated Multi-ChatClient Configuration:\n\n\`\`\`java\n${code}\n\`\`\`\n\nKey features:\n- Multiple ChatClient beans for different strategies\n- Default system prompts for each client\n- Use @Qualifier annotation to inject specific client\n- Follows current Spring AI best practices`,
                    resultType: "success"
                };
            }
        },
        {
            name: "generate_structured_output_service",
            description: "Generate a service for extracting structured data from AI responses using entity mapping",
            parameters: {
                type: "object",
                properties: {
                    className: {
                        type: "string",
                        description: "Service class name (e.g., DataExtractorService)"
                    },
                    entityName: {
                        type: "string",
                        description: "Name of the entity class for structured output (e.g., Trade, UserInfo)"
                    }
                },
                required: ["className", "entityName"]
            },
            handler: async (args) => {
                const code = SPRING_AI_PATTERNS.structuredOutputService
                    .replace(/{{className}}/g, args.className)
                    .replace(/{{entityName}}/g, args.entityName);
                
                return {
                    textResultForLlm: `Generated Structured Output Service:\n\n\`\`\`java\n${code}\n\`\`\`\n\nKey features:\n- Uses entity() method for single object mapping\n- Supports List<T> extraction with ParameterizedTypeReference\n- Type-safe structured outputs\n- Reduces string parsing overhead`,
                    resultType: "success"
                };
            }
        },
        {
            name: "generate_chatresponse_service",
            description: "Generate a service that captures detailed ChatResponse metadata (tokens, model info, etc.)",
            parameters: {
                type: "object",
                properties: {
                    className: {
                        type: "string",
                        description: "Service class name (e.g., AnalyticsService, MetricsService)"
                    }
                },
                required: ["className"]
            },
            handler: async (args) => {
                const code = SPRING_AI_PATTERNS.chatResponseService
                    .replace(/{{className}}/g, args.className);
                
                return {
                    textResultForLlm: `Generated ChatResponse Service:\n\n\`\`\`java\n${code}\n\`\`\`\n\nKey features:\n- Accesses ChatResponse object for metadata\n- Captures token usage information\n- Logs and monitors AI model usage\n- Useful for cost tracking and observability`,
                    resultType: "success"
                };
            }
        },
        {
            name: "generate_streaming_service",
            description: "Generate a service that streams AI responses using Spring AI's reactive Flux API",
            parameters: {
                type: "object",
                properties: {
                    className: {
                        type: "string",
                        description: "Service class name (e.g., StreamingService, RealtimeService)"
                    }
                },
                required: ["className"]
            },
            handler: async (args) => {
                const code = SPRING_AI_PATTERNS.streamingService
                    .replace(/{{className}}/g, args.className);
                
                return {
                    textResultForLlm: `Generated Streaming Service:\n\n\`\`\`java\n${code}\n\`\`\`\n\nKey features:\n- Uses stream() instead of call() for streaming\n- Returns Flux<String> for reactive responses\n- Real-time content delivery to clients\n- Works with Server-Sent Events (SSE)\n- Spring AI reactive support`,
                    resultType: "success"
                };
            }
        },
        {
            name: "spring_ai_best_practices",
            description: "Return current Spring AI best practices, patterns, and resource links",
            parameters: {
                type: "object",
                properties: {
                    topic: {
                        type: "string",
                        enum: ["chatclient", "structured-output", "multiple-models", "streaming", "configuration", "resources"],
                        description: "The topic to get best practices for"
                    }
                },
                required: ["topic"]
            },
            handler: async (args) => {
                const practices = {
                    chatclient: `Spring AI ChatClient Best Practices:\n\n1. Constructor Injection: Always inject ChatClient.Builder via constructor for dependency management\n2. Fluent API: Use the fluent API (chatClient.prompt().user(...).call().content())\n3. System Prompts: Set default system prompts in configuration for consistent behavior\n4. Error Handling: Handle provider/API failures and expose clear errors\n5. Logging: Use SLF4J Logger for debugging and auditing\n6. Spring Boot Compatibility: Align your Spring Boot and Spring AI versions with the official compatibility matrix\n7. ChatClient is thread-safe: Can be injected as a singleton bean`,
                    
                    "structured-output": `Spring AI Structured Output Best Practices:\n\n1. Use entity() method: chatClient.prompt().user(...).call().entity(YourClass.class)\n2. Create Records: Use Java records for simple data structures\n3. Generics Support: Use ParameterizedTypeReference<List<T>> for List extraction\n4. Native Structured Output: Use AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT when supported\n5. Type Safety: Prefer strongly-typed entities over String parsing\n6. Validation: Add validation annotations (@NotNull, @Size, etc.) to entity fields\n7. Error Recovery: Implement explicit handling for malformed model output`,
                    
                    "multiple-models": `Spring AI Multiple ChatClient Best Practices:\n\n1. Disable Auto-config: Set spring.ai.chat.client.enabled=false if managing ChatClients manually\n2. Use @Configuration: Create configuration classes to define multiple ChatClient beans\n3. Use @Bean: Create separate beans for each ChatClient variant (fast, advanced, default)\n4. Use @Qualifier: Inject specific ChatClient with @Qualifier("beanName")\n5. Customize System Prompts: Each ChatClient can have different default system prompts\n6. Model-Specific Options: Use builder.defaultOptions(...) for model-specific configuration\n7. Fallback Strategy: Implement fallback to alternative model if primary fails`,
                    
                    streaming: `Spring AI Streaming Best Practices:\n\n1. Use stream() instead of call(): For streaming responses\n2. Return Flux<String>: For reactive/non-blocking responses\n3. Server-Sent Events: Stream results to browser clients with SSE\n4. Backpressure: Flux handles backpressure automatically\n5. Error Handling: Subscribe with .doOnError() for error handling\n6. Completion: Flux signals completion automatically\n7. Performance: Streaming is more responsive for long-running operations`,
                    
                    configuration: `Spring AI Configuration Best Practices:\n\n1. application.properties: Store Spring AI configuration in properties file\n2. Model Selection: spring.ai.ollama.model=llama3:8b or spring.ai.openai.api-key=...\n3. Temperature: Control randomness with spring.ai.chat.client.default-options.temperature\n4. Provider-Specific: Each provider has unique configuration options\n5. Environment Variables: Use environment variables for sensitive API keys\n6. Spring Profiles: Use application-{profile}.properties for env-specific configs\n7. Custom Properties: Create @ConfigurationProperties classes for complex configs`,

                    resources: `Spring AI Reference Resources:\n\n1. Spring AI Project: https://spring.io/projects/spring-ai\n2. Spring AI Reference Docs: https://docs.spring.io/spring-ai/reference/\n3. Spring AI API Docs: https://docs.spring.io/spring-ai/docs/1.0.0-SNAPSHOT/api/\n4. Spring AI 1.0 GA Blog: https://spring.io/blog/2025/05/20/spring-ai-1-0-GA-released/\n5. Awesome Spring AI Curated List: https://github.com/spring-ai-community/awesome-spring-ai\n6. First Spring AI 1.0 App Tutorial: https://spring.io/blog/2025/05/20/your-first-spring-ai-1\n7. Spring AI Agentic Patterns: https://spring.io/blog/2025/01/21/spring-ai-agentic-patterns`
                };
                
                return {
                    textResultForLlm: practices[args.topic] || "Topic not found",
                    resultType: "success"
                };
            }
        }
    ],
    hooks: {},
});
