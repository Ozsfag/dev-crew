package org.blacksoil.devcrew.notification.adapter.out.telegram;

import java.util.List;
import org.blacksoil.devcrew.notification.adapter.out.telegram.dto.TelegramUpdate;

/**
 * Port для вызова Telegram Bot API. Изолирует HTTP-слой от бизнес-логики уведомлений — позволяет
 * мокировать в тестах.
 */
public interface TelegramApiClient {

  void sendMessage(String chatId, String text);

  /** Получает список обновлений начиная с offset (long-polling, timeout=0). */
  List<TelegramUpdate> getUpdates(long offset);

  /** Скачивает содержимое файла по fileId. Возвращает пустой массив при ошибке. */
  byte[] downloadFile(String fileId);
}
