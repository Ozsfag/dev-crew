package org.blacksoil.devcrew.notification.adapter.out.telegram.dto;

/** Обёртка для ответа Telegram Bot API. */
public record TelegramApiResponse<T>(boolean ok, T result) {}
