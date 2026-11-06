# Streaming Chatbot (Spring Boot + Spring AI + Ollama)

A simple AI chatbot that runs **fully on your own computer**. You type a message in the browser, and the answer appears **word by word** (streaming), just like ChatGPT.

No API key and no internet AI service is needed. The AI model (Llama 3.1) runs locally through [Ollama](https://ollama.com).

---

## What the project does

1. You type a question on the web page.
2. The page sends it to the Spring Boot server.
3. The server forwards it to the local Ollama model.
4. The model generates the answer token by token.
5. The server streams each token back, and the page shows it as it arrives.

---

## How it works

```
Browser (index.html)
      |
      |  GET /stream?message=Hello
      v
Spring Boot server (ChatController)
      |
      |  Spring AI ChatClient  .stream()
      v
Ollama (llama3.1) on localhost:11434
      |
      |  tokens, one by one
      v
Server sends them as Server-Sent Events (SSE)
      |
      v
Browser reads each chunk and updates the screen live
```

### Why it streams

| Part | What makes it stream |
|------|----------------------|
| Backend | `.stream().content()` returns a `Flux<String>`, and `produces = text/event-stream` sends each token immediately |
| Frontend | `response.body.getReader()` with a `while` loop reads each chunk as it arrives and updates the page |

> Using `response.text()` on the frontend would break streaming, because it waits for the full response.

---

## Tech stack

- **Java 17**
- **Spring Boot 4.1.2** (Spring Web MVC)
- **Spring AI 2.0.1** (Ollama starter)
- **Ollama** with the **llama3.1** model
- **HTML + Tailwind CSS** for the page
- **marked** and **DOMPurify** to show Markdown safely
- **Maven** for building

---

## Project structure

```
Streaming_Chatbot/
├── pom.xml                         # Maven dependencies
└── src/main/
    ├── java/com/chatbot/
    │   ├── (Application class)     # Starts the Spring Boot app
    │   └── ChatController.java     # /stream endpoint, talks to Ollama
    └── resources/
        ├── application.properties  # Port and model settings
        └── static/
            └── index.html          # Chat page (frontend)
```

---

## Before you start

Install:

1. **Java 17 or higher**
2. **Maven** (or use the Maven wrapper if your project has one)
3. **Ollama**: https://ollama.com/download

Then download the model:

```bash
ollama pull llama3.1
```

Check it is installed:

```bash
ollama list
```

---

## Configuration

`src/main/resources/application.properties`

```properties
spring.application.name=Streaming_Chatbot
server.port=8082

# Ollama
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.options.model=llama3.1

# Do not cut long answers from a slow local model
spring.mvc.async.request-timeout=300s
```

To use another model, run `ollama pull <model>` and change the model name above.

---

## How to run

1. Make sure Ollama is running (`ollama serve`, or the Ollama desktop app).
2. In the project folder, start the app:

   ```bash
   mvn spring-boot:run
   ```

3. Open your browser:

   ```
   http://localhost:8082
   ```

4. Type a message and press **Send**. The answer should appear gradually.

---

## The API endpoint

| Method | URL | Description |
|--------|-----|-------------|
| GET | `/stream?message=your question` | Streams the AI answer as Server-Sent Events |

Quick test from a terminal:

```bash
curl -N "http://localhost:8082/stream?message=Hello"
```

The `-N` option turns off buffering so you can see the tokens arrive.

---

## Key code

**Backend** (`ChatController.java`)

```java
@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> chatStream(@RequestParam String message) {
    return chatClient.prompt()
            .system("...rules for clean Markdown answers...")
            .user(message)
            .stream()
            .content();
}
```

**Frontend** (`index.html`)

- `fetch("/stream?message=...")` opens the connection.
- `response.body.getReader()` reads chunks as they arrive.
- Each `data:` line is read **without trimming**, because a leading space belongs to the token.
- Lines of the same event are joined with `\n` so Markdown keeps its line breaks.
- `marked` converts the text to HTML and `DOMPurify` cleans it before showing.

---

## Troubleshooting

| Problem | Likely cause and fix |
|---------|----------------------|
| `Connection refused` to `localhost:11434` | Ollama is not running. Start it with `ollama serve`. |
| `model not found` | Run `ollama pull llama3.1` and check the model name in `application.properties`. |
| Whole answer appears at once | Hard refresh the page (Ctrl+Shift+R), restart the app, and make sure the frontend uses `getReader()` and not `response.text()`. |
| Words stick together (`Helloworld`) | Do not use `trimStart()` on the `data:` line. |
| Answer stops halfway | Increase `spring.mvc.async.request-timeout`. |
| First answer is slow | Ollama is loading the model into memory. Later answers are faster. |
| Page not loading | Check the app is on port 8082 and `index.html` is in `src/main/resources/static/`. |

---

## Current limitations and ideas for the future

- The bot has **no memory**. Each message is answered on its own.
- No login or multiple users.
- Ideas: chat history with Spring AI chat memory, a model selector, a "stop generating" button, and saving conversations to a database.
