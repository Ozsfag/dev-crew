package org.blacksoil.devcrew.agent.bootstrap;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import org.blacksoil.devcrew.agent.adapter.out.llm.tools.DockerTools;
import org.blacksoil.devcrew.agent.adapter.out.llm.tools.FileTools;
import org.blacksoil.devcrew.agent.adapter.out.llm.tools.GitTools;
import org.blacksoil.devcrew.agent.adapter.out.llm.tools.GradleTools;
import org.blacksoil.devcrew.agent.app.config.AgentProperties;
import org.blacksoil.devcrew.agent.domain.BackendDevAgent;
import org.blacksoil.devcrew.agent.domain.CodeReviewAgent;
import org.blacksoil.devcrew.agent.domain.DevOpsAgent;
import org.blacksoil.devcrew.agent.domain.QaAgent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Регистрирует LangChain4j AiServices-агентов как Spring-бины. ChatLanguageModel настраивается
 * через langchain4j.anthropic.* в application.yml.
 */
@Configuration
public class LangChain4jAgentConfig {

  @Bean
  public BackendDevAgent backendDevAgent(
      ChatLanguageModel chatLanguageModel,
      AgentProperties agentProperties,
      FileTools fileTools,
      GitTools gitTools,
      GradleTools gradleTools) {
    return AiServices.builder(BackendDevAgent.class)
        .chatLanguageModel(chatLanguageModel)
        .tools(fileTools, gitTools, gradleTools)
        .build();
  }

  @Bean
  public QaAgent qaAgent(
      ChatLanguageModel chatLanguageModel,
      FileTools fileTools,
      GitTools gitTools,
      GradleTools gradleTools) {
    return AiServices.builder(QaAgent.class)
        .chatLanguageModel(chatLanguageModel)
        .tools(fileTools, gitTools, gradleTools)
        .build();
  }

  @Bean
  public CodeReviewAgent codeReviewAgent(
      ChatLanguageModel chatLanguageModel, FileTools fileTools, GitTools gitTools) {
    return AiServices.builder(CodeReviewAgent.class)
        .chatLanguageModel(chatLanguageModel)
        .tools(fileTools, gitTools)
        .build();
  }

  @Bean
  public DevOpsAgent devOpsAgent(
      ChatLanguageModel chatLanguageModel,
      FileTools fileTools,
      GitTools gitTools,
      DockerTools dockerTools) {
    return AiServices.builder(DevOpsAgent.class)
        .chatLanguageModel(chatLanguageModel)
        .tools(fileTools, gitTools, dockerTools)
        .build();
  }
}
