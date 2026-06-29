package org.blacksoil.devcrew.agent.domain.model;

/**
 * Результат продолжения интерактивного сеанса claude.
 *
 * @param result текстовый ответ агента
 * @param sessionId id сеанса claude — передаётся в следующий вызов через --resume для сохранения
 *     контекста диалога
 */
public record SessionResult(String result, String sessionId) {}
