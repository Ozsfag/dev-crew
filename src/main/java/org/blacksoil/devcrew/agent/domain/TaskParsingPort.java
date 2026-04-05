package org.blacksoil.devcrew.agent.domain;

import org.blacksoil.devcrew.agent.domain.model.ParsedTask;

/** Port для парсинга свободного текста пользователя в структурированную задачу через LLM. */
public interface TaskParsingPort {

  ParsedTask parse(String userMessage);
}
