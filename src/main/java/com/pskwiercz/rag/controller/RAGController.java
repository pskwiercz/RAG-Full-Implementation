package com.pskwiercz.rag.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rag")
public class RAGController {

    private final ChatClient webSearchchatClient;
    private final VectorStore vectorStore;

    public RAGController(@Qualifier("webSearchRAGChatClient") ChatClient webSearchchatClient,
                         VectorStore vectorStore) {
        this.webSearchchatClient = webSearchchatClient;
        this.vectorStore = vectorStore;
    }

    @GetMapping("/web-search/chat")
    public ResponseEntity<String> webSearchChat(@RequestHeader("username")
                                                String username, @RequestParam("message") String message) {
        String answer = webSearchchatClient.prompt()
                .advisors(a -> a.param(CONVERSATION_ID, username))
                .user(message)
                .call().content();
        return ResponseEntity.ok(answer);
    }
}
