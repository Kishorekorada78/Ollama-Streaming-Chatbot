package com.chatbot;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestParam String message) {

        return chatClient.prompt()
                .system("""
                        You are a helpful AI assistant.

                        Answer using clean and well-structured Markdown.

                        Rules:
                        - Use short paragraphs.
                        - Use headings only when necessary.
                        - Use bullet points for lists.
                        - Avoid unnecessarily long answers.
                        - Use simple English.
                        - Do not repeat the question.
                        - Use code blocks for programming code.
                        """)
                .user(message)
                .stream()
                .content();
    }
}