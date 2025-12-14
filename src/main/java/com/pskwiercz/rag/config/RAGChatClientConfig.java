package com.pskwiercz.rag.config;

import com.pskwiercz.rag.advisor.TokenUsageAdvisor;
import com.pskwiercz.rag.tools.PIIMaskProcessor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class RAGChatClientConfig {

    @Bean("ragController")
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory
            ,RetrievalAugmentationAdvisor  retrievalAugmentationAdvisor) {
        Advisor loggerAdvisor = new SimpleLoggerAdvisor();
        Advisor tokenUsageAdvisor = new TokenUsageAdvisor();
        Advisor memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();

        return chatClientBuilder
                .defaultAdvisors(List.of(loggerAdvisor, memoryAdvisor, tokenUsageAdvisor, retrievalAugmentationAdvisor))
                .build();
    }

    @Bean
    RetrievalAugmentationAdvisor retrievalAugmentationAdvisor(VectorStore vectorStore,
                                                              ChatClient.Builder chatClientBuilder) {
        return RetrievalAugmentationAdvisor.builder()
                // Translate query to english
                .queryTransformers(TranslationQueryTransformer.builder()
                        .chatClientBuilder(chatClientBuilder.clone())
                        .targetLanguage("english").build())
                // serach vector DB
                .documentRetriever(VectorStoreDocumentRetriever.builder().vectorStore(vectorStore)
                        .topK(3).similarityThreshold(0.5).build())
                // mask PII in response
                .documentPostProcessors(PIIMaskProcessor.builder())
                .build();
    }
}
