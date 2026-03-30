package org.blacksoil.devcrew.agent.bootstrap;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import org.blacksoil.devcrew.agent.adapter.out.llm.tools.FileTools;
import org.blacksoil.devcrew.agent.adapter.out.llm.tools.GradleTools;
import org.blacksoil.devcrew.agent.app.config.AgentProperties;
import org.blacksoil.devcrew.agent.domain.BackendDevAgent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Регистрирует LangChain4j AiServices-агентов как Spring-бины.
 * ChatLanguageModel настраивается через langchain4j.anthropic.* в application.yml.
 */
@Configuration
public class LangChain4jAgentConfig {

    @Bean
    public BackendDevAgent backendDevAgent(
        ChatLanguageModel chatLanguageModel,
        AgentProperties agentProperties,
        FileTools fileTools,
        GradleTools gradleTools
    ) {
        return AiServices.builder(BackendDevAgent.class)
            .chatLanguageModel(chatLanguageModel)
            .tools(fileTools, gradleTools)
            .build();
    }
}
