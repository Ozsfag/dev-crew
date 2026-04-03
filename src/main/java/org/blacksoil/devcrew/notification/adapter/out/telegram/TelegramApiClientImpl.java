package org.blacksoil.devcrew.notification.adapter.out.telegram;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;

/**
 * Реализация TelegramApiClient через Spring RestClient. Создаётся в TelegramConfig как Spring-бин.
 */
@Slf4j
@RequiredArgsConstructor
public class TelegramApiClientImpl implements TelegramApiClient {

  private static final String API_URL = "https://api.telegram.org/bot";

  private final RestClient restClient;
  private final String botToken;

  @Override
  public void sendMessage(String chatId, String text) {
    try {
      restClient
          .post()
          .uri(API_URL + botToken + "/sendMessage")
          .body(Map.of("chat_id", chatId, "text", text))
          .retrieve()
          .toBodilessEntity();
    } catch (Exception e) {
      log.error("Не удалось отправить Telegram-сообщение", e);
    }
  }
}
