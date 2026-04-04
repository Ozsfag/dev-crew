package org.blacksoil.devcrew.notification.app.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.agent.domain.agent.TaskParserAgent;
import org.blacksoil.devcrew.agent.domain.model.ParsedTask;
import org.springframework.stereotype.Service;

/** Парсит свободный текст пользователя в структурированную задачу через LLM. */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskParserService {

  private final TaskParserAgent taskParserAgent;
  private final ObjectMapper objectMapper;

  /**
   * Парсит текст в ParsedTask. При ошибке десериализации возвращает задачу с пустым title (сигнал
   * для вызывающего кода отправить пользователю сообщение об ошибке).
   */
  public ParsedTask parse(String userMessage) {
    try {
      var json = taskParserAgent.parse(userMessage);
      return objectMapper.readValue(json, ParsedTask.class);
    } catch (Exception e) {
      log.warn("Не удалось разобрать ответ TaskParserAgent: {}", e.getMessage());
      return new ParsedTask("", userMessage, AgentRole.BACKEND_DEV);
    }
  }
}
