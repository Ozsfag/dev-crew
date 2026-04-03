package org.blacksoil.devcrew.agent.app.config;

import org.blacksoil.devcrew.agent.app.policy.SandboxPolicy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({AgentProperties.class, SandboxProperties.class})
public class AgentConfig {

  @Bean
  public SandboxPolicy sandboxPolicy(SandboxProperties properties) {
    return new SandboxPolicy(properties.getRoot());
  }
}
