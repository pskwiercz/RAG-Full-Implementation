package com.pskwiercz.rag.config;

import com.pskwiercz.rag.advisor.CompositeAdvisor;
import com.pskwiercz.rag.advisor.TokenUsageAdvisor;
import com.pskwiercz.rag.web.WebSearchDocumentRetriever;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.util.List;

@Configuration
public class CompositeRAGChatClientConfig {

    @Bean("compositeRagController")
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory,
                                 QdrantVectorStore vectorStore,
                                 RestClient.Builder restClientBuilder) {

        Advisor loggerAdvisor = new SimpleLoggerAdvisor();
        Advisor tokenUsageAdvisor = new TokenUsageAdvisor();
        Advisor memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();

        Advisor compositeAdvisor = new CompositeAdvisor(
                WebSearchDocumentRetriever.builder()
                        .restClientBuilder(restClientBuilder)
                        .maxResults(2)
                        .build(),
                VectorStoreDocumentRetriever.builder()
                        .vectorStore(vectorStore)
                        .topK(3)
                        .similarityThreshold(0.5)
                        .build());

        return chatClientBuilder
                .defaultAdvisors(List.of(loggerAdvisor, memoryAdvisor, tokenUsageAdvisor, compositeAdvisor))
                .build();
    }

}
