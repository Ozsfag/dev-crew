package org.blacksoil.devcrew.notification.adapter.out.telegram.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Голосовое сообщение Telegram. */
public record TelegramVoice(@JsonProperty("file_id") String fileId) {}
