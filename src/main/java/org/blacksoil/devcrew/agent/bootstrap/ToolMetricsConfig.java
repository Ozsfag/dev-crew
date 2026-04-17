package org.blacksoil.devcrew.agent.bootstrap;

import io.micrometer.core.instrument.MeterRegistry;
import org.blacksoil.devcrew.agent.adapter.out.llm.process.CommandRunner;
import org.blacksoil.devcrew.agent.adapter.out.llm.process.MeteredCommandRunner;
import org.blacksoil.devcrew.agent.adapter.out.llm.process.ProcessBuilderCommandRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Оборачивает CommandRunner в MeteredCommandRunner — декоратор с метриками Micrometer. Все
 * Tool-бины (GradleTools, GitTools, DockerTools) автоматически получают метрики через @Primary.
 */
@Configuration
public class ToolMetricsConfig {

  @Bean
  @Primary
  public CommandRunner meteredCommandRunner(
      ProcessBuilderCommandRunner delegate, MeterRegistry meterRegistry) {
    return new MeteredCommandRunner(delegate, meterRegistry);
  }
}
