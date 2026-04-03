package org.blacksoil.devcrew.notification.adapter.out.telegram;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Конфигурация Telegram-бота через application.yml (devcrew.notification.telegram.*). */
@Data
@ConfigurationProperties(prefix = "devcrew.notification.telegram")
public class TelegramProperties {

  /** Включить отправку уведомлений. По умолчанию выключено. */
  private boolean enabled = false;

  /** Токен Telegram Bot API. Задаётся через env-переменную TELEGRAM_BOT_TOKEN. */
  private String botToken = "";

  /** ID чата архитектора. Задаётся через env-переменную TELEGRAM_CHAT_ID. */
  private String chatId = "";

  /** Максимальная длина сообщения Telegram API. */
  private int maxMessageLength = 4096;
}
