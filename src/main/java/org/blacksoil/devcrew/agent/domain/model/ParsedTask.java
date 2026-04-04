package org.blacksoil.devcrew.agent.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.blacksoil.devcrew.agent.domain.AgentRole;

/**
 * Результат парсинга свободного текста пользователя в структурированную задачу. Возвращается
 * TaskParserAgent как JSON и десериализуется в этот record.
 */
public record ParsedTask(
    String title, String description, @JsonProperty("agentRole") AgentRole agentRole) {}
