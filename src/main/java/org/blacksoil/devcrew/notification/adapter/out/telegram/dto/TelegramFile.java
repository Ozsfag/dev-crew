package org.blacksoil.devcrew.notification.adapter.out.telegram.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Файл Telegram (ответ getFile). */
public record TelegramFile(@JsonProperty("file_path") String filePath) {}
