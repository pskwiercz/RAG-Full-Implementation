package com.pskwiercz.rag.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rag")
public class RAGController {

    private final ChatClient webSearchChatClient;
    private final ChatClient ragChatClient;
    private final ChatClient compositeChatClient;
    private final VectorStore vectorStore;


    @Value("classpath:/templates/systemPrompt.st")
    Resource hrSystemTemplate;

    public RAGController(@Qualifier("webSearchRAGChatClient") ChatClient webSearchchatClient,
                         @Qualifier("ragController") ChatClient ragChatClient,
                         @Qualifier("compositeRagController") ChatClient compositeChatClient,
                         VectorStore vectorStore) {
        this.webSearchChatClient = webSearchchatClient;
        this.ragChatClient = ragChatClient;
        this.compositeChatClient = compositeChatClient;
        this.vectorStore = vectorStore;
    }

    @GetMapping("/rag/chat")
    public ResponseEntity<String> documentChat(@RequestHeader("username") String username,
                                               @RequestParam("message") String message) {

//       SearchRequest searchRequest =
//                SearchRequest.builder().query(message).topK(3).similarityThreshold(0.5).build();
//        List<Document> similarDocs =  vectorStore.similaritySearch(searchRequest);
//        String similarContext = similarDocs.stream()
//                .map(Document::getText)
//                .collect(Collectors.joining(System.lineSeparator()));
        String answer = ragChatClient.prompt()
//                .system(promptSystemSpec -> promptSystemSpec.text(hrSystemTemplate)
//                                .param("documents", similarContext))
                .advisors(a -> a.param(CONVERSATION_ID, username))
                .user(message)
                .call().content();
        return ResponseEntity.ok(answer);
    }

    @GetMapping("/web-search/chat")
    public ResponseEntity<String> webSearchChat(@RequestHeader("username") String username,
                                                @RequestParam("message") String message) {
        String answer = webSearchChatClient.prompt()
                .advisors(a -> a.param(CONVERSATION_ID, username))
                .user(message)
                .call().content();
        return ResponseEntity.ok(answer);
    }

    @GetMapping("/web-rag/chat")
    public ResponseEntity<String> webRagChat(@RequestHeader("username") String username,
                                             @RequestParam("message") String message) {
        String answer = compositeChatClient.prompt()
                .advisors(a -> a.param(CONVERSATION_ID, username))
                .user(message)
                .call().content();
        return ResponseEntity.ok(answer);
    }
}
