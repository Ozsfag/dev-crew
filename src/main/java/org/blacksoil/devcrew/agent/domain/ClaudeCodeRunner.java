package org.blacksoil.devcrew.agent.domain;

/**
 * Port для запуска Claude Code CLI как subprocess. Изолирует детали вызова claude CLI от
 * бизнес-логики.
 */
public interface ClaudeCodeRunner {

  /**
   * Запускает claude CLI с заданным системным промптом и пользовательским заданием.
   *
   * @param systemPrompt содержимое CLAUDE.md — роль и инструкции агента
   * @param userPrompt задание для агента
   * @return результат выполнения агента
   */
  String run(String systemPrompt, String userPrompt);

  /**
   * Запускает claude CLI с явным ограничением числа ходов (для задач без инструментов).
   *
   * @param systemPrompt содержимое CLAUDE.md
   * @param userPrompt задание для агента
   * @param maxTurns максимальное число ходов (1 — только текстовый ответ, без инструментов)
   * @return результат выполнения агента
   */
  String run(String systemPrompt, String userPrompt, int maxTurns);
}
