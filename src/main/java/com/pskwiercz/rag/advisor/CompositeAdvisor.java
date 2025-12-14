package com.pskwiercz.rag.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class CompositeAdvisor implements CallAdvisor {

    private final Resource hrSystemTemplate = new ClassPathResource("templates/systemPrompt.st");

    private final DocumentRetriever webRetriever;
    private final DocumentRetriever vectorRetriever;

    public CompositeAdvisor(DocumentRetriever webRetriever,
                            DocumentRetriever vectorRetriever) {
        this.webRetriever = webRetriever;
        this.vectorRetriever = vectorRetriever;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        Query userQuery = new Query(chatClientRequest.prompt().getUserMessage().getText());

        // Web search
        List<Document> webDocs = webRetriever.retrieve(userQuery);

        // Vector DB search
        List<Document> vectorDocs = vectorRetriever.retrieve(userQuery);

        // Merge all retrieved docs
        List<Document> allDocs = new ArrayList<>();
        allDocs.addAll(webDocs);
        allDocs.addAll(vectorDocs);

        chatClientRequest.prompt().augmentSystemMessage(createPrompt(allDocs));

        return callAdvisorChain.nextCall(chatClientRequest);
    }

    @Override
    public String getName() {
        return "Composite Advisor";
    }

    @Override
    public int getOrder() {
        return 0;
    }

    private String createPrompt(List<Document> allDocs) {

        try {
            // Build a combined context string
            String documents = allDocs.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining("\n------\n"));

            String template = new String(hrSystemTemplate.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            PromptTemplate prompt = PromptTemplate.builder().template(template).build();
            return prompt.create(Map.of("documents", documents)).getContents();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
