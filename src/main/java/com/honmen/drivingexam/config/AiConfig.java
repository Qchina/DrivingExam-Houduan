package com.honmen.drivingexam.config;

import com.honmen.drivingexam.service.ai.DrivingExamAiAssistant;
import com.honmen.drivingexam.service.ai.DrivingExamAiTools;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {
    @Bean
    public OpenAiChatModel drivingExamChatModel(
        @Value("${ai.api-key:}") String apiKey,
        @Value("${ai.base-url:https://api.deepseek.com}") String baseUrl,
        @Value("${ai.model:deepseek-v4-pro}") String model
    ) {
        return OpenAiChatModel.builder()
            .baseUrl(baseUrl)
            .apiKey(apiKey)
            .modelName(model)
            .temperature(0.3)
            .build();
    }

    @Bean
    public DrivingExamAiAssistant drivingExamAiAssistant(
        OpenAiChatModel chatModel,
        DrivingExamAiTools tools
    ) {
        return AiServices.builder(DrivingExamAiAssistant.class)
            .chatModel(chatModel)
            .tools(tools)
            .build();
    }
}
