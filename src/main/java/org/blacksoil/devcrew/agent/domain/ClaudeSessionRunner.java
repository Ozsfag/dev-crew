package org.blacksoil.devcrew.agent.domain;

import org.blacksoil.devcrew.agent.domain.model.SessionResult;

/**
 * Port для интерактивного диалога с claude CLI в папке реального репозитория. В отличие от {@link
 * ClaudeCodeRunner} (одноразовый запуск роли во временной папке) сохраняет контекст между вызовами
 * через session_id — это «Claude Code в терминале», управляемый из Telegram.
 */
public interface ClaudeSessionRunner {

  /**
   * Продолжает (или начинает) сеанс claude в указанном репозитории.
   *
   * @param workDir папка репозитория — claude читает её собственный CLAUDE.md и правит её файлы
   * @param sessionId id предыдущего сеанса для продолжения контекста; {@code null} — начать новый
   * @param userMessage сообщение пользователя
   * @return ответ агента и id сеанса для следующего вызова
   */
  SessionResult continueSession(String workDir, String sessionId, String userMessage);
}
