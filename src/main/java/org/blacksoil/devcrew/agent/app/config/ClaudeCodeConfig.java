package org.blacksoil.devcrew.agent.app.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Активирует ClaudeCodeProperties. */
@Configuration
@EnableConfigurationProperties(ClaudeCodeProperties.class)
public class ClaudeCodeConfig {}
