package org.blacksoil.devcrew.notification.adapter.out.telegram;

/**
 * Port для вызова Telegram Bot API. Изолирует HTTP-слой от бизнес-логики уведомлений — позволяет
 * мокировать в тестах.
 */
public interface TelegramApiClient {

  void sendMessage(String chatId, String text);
}
