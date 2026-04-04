package org.blacksoil.devcrew.notification.adapter.out.telegram.dto;

/** Входящее сообщение Telegram. */
public record TelegramMessage(TelegramChat chat, String text, TelegramVoice voice) {}
