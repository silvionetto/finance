# Spring AI Skill - How to Use

## Your Skill Is Ready

The **Spring AI Code Generator** extension is loaded and running with 7 code generation tools.

---

## Quick Commands

Try any of these commands immediately in your Copilot CLI session:

### Create a Service
```
generate_spring_ai_service className=MyService methodName=process
```
Generates a Spring Service with ChatClient

### Create a Controller
```
generate_spring_ai_controller className=MyController
```
Generates a REST Controller with ChatClient endpoints

### Create Configuration
```
generate_multi_chatclient_config className=AppConfig
```
Generates Spring @Configuration with 3 ChatClient beans

### Extract Structured Data
```
generate_structured_output_service className=MyExtractor entityName=MyData
```
Generates type-safe data extraction service

### Track Token Usage
```
generate_chatresponse_service className=TokenTracker
```
Generates token usage and cost monitoring service

### Stream Responses
```
generate_streaming_service className=RealtimeService
```
Generates real-time Flux streaming service

### Get Best Practices
```
spring_ai_best_practices topic=chatclient
```
Returns best practices for: chatclient, structured-output, multiple-models, streaming, configuration
Use `spring_ai_best_practices topic=resources` for curated links from official docs and awesome-spring-ai.

---

## Real-World Examples

### Example 1: Email Analysis Service
```
generate_spring_ai_service className=EmailAnalyzerService methodName=analyzeEmail
```
**Use**: Analyze incoming emails with LLM

### Example 2: Trade Extraction
```
generate_structured_output_service className=TradeExtractorService entityName=Trade
```
**Use**: Extract structured trade data from emails

### Example 3: Script Review API
```
generate_spring_ai_controller className=ScriptReviewController
```
**Use**: REST endpoint for video script review

### Example 4: Multi-Model Support
```
generate_multi_chatclient_config className=AiModelConfiguration
```
**Use**: Support multiple LLM models in one app

### Example 5: Real-Time Updates
```
generate_streaming_service className=RealtimeAiService
```
**Use**: Server-Sent Events for live script feedback

### Example 6: Email Reply Service
```
generate_spring_ai_service className=EmailReplyService methodName=generateReply
```
**Use**: Generate automated email replies

### Example 7: Cost Tracking
```
generate_chatresponse_service className=CostTrackerService
```
**Use**: Monitor token usage and LLM costs

---

## Documentation

### First Time?
1. Start with: `.github/extensions/spring-ai-code-generator/README.md`
2. Then read: `.github/extensions/spring-ai-code-generator/GUIDE.md`

### Need Examples?
- See GUIDE.md for complete code examples for all 7 tools

### Need Best Practices?
```
spring_ai_best_practices topic=structured-output
```

### Need Configuration?
See GUIDE.md "Configuration Reference" section

---

## File Locations

```
.github/extensions/spring-ai-code-generator/
├── extension.mjs       ← Main extension (don't edit)
├── README.md           ← Quick reference
└── GUIDE.md            ← Comprehensive guide
```

---

## What Each Tool Does

| Tool | Returns | For |
|------|---------|-----|
| generate_spring_ai_service | Service class code | ChatClient services |
| generate_spring_ai_controller | Controller class code | REST endpoints |
| generate_multi_chatclient_config | @Configuration class | Multiple models |
| generate_structured_output_service | Service with entity mapping | Data extraction |
| generate_chatresponse_service | Service with metadata access | Token tracking |
| generate_streaming_service | Service with Flux streaming | Real-time updates |
| spring_ai_best_practices | Text guide | Learning/reference |

---

## Copy-Paste Workflow

1. Run the tool command above
2. Copy the generated Java code
3. Paste into your src/main/java directory
4. Adjust package name if needed
5. Implement business logic
6. Run tests
7. Deploy

---

## Based On

- **Spring AI Reference**: https://docs.spring.io/spring-ai/reference/
- **Awesome Spring AI** curated resources
- **Spring Boot 4.0+** conventions
- **Java 21+** syntax

---

## Need Help?

- 📖 **Quick Reference**: README.md
- 📚 **Deep Dive**: GUIDE.md
- 🎯 **Best Practices**: `spring_ai_best_practices`
- 💡 **Examples**: This file

---

## Spring AI Concepts

**ChatClient** - Main interface for LLM communication
```java
chatClient.prompt().user("Hello").call().content()
```

**Structured Output** - Type-safe entity mapping
```java
MyEntity entity = chatClient.prompt(...).call().entity(MyEntity.class)
```

**Streaming** - Real-time responses
```java
Flux<String> stream = chatClient.prompt(...).stream().content()
```

**Multiple Models** - Different ChatClients for different tasks
```java
@Qualifier("fastClient") ChatClient fast;
@Qualifier("advancedClient") ChatClient advanced;
```

---

## All Tools Available Right Now

✅ generate_spring_ai_service
✅ generate_spring_ai_controller
✅ generate_multi_chatclient_config
✅ generate_structured_output_service
✅ generate_chatresponse_service
✅ generate_streaming_service
✅ spring_ai_best_practices

---

## Ready to Start?

Pick one example above and try it. All generated code is:
- Production-ready
- Copy-paste compatible
- Spring AI project-compatible
- Best-practice compliant
- Fully documented in GUIDE.md

**Start now!** 🚀
