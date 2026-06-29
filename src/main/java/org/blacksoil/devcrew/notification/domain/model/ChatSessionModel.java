package org.blacksoil.devcrew.notification.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Состояние диалога Telegram-чата: какой проект выбран и id сеанса claude для продолжения
 * контекста.
 *
 * @param current выбран ли этот проект как активный для чата прямо сейчас
 * @param claudeSessionId id сеанса claude; {@code null} — следующее сообщение начнёт новый диалог
 */
public record ChatSessionModel(
    UUID id,
    long chatId,
    String projectName,
    String claudeSessionId,
    boolean current,
    Instant createdAt,
    Instant updatedAt) {}
