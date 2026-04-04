package org.blacksoil.devcrew.notification.adapter.out.telegram.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Одно обновление из Telegram getUpdates. */
public record TelegramUpdate(@JsonProperty("update_id") long updateId, TelegramMessage message) {}
