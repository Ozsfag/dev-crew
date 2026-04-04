package org.blacksoil.devcrew.notification.adapter.out.telegram;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.blacksoil.devcrew.notification.adapter.out.telegram.dto.TelegramApiResponse;
import org.blacksoil.devcrew.notification.adapter.out.telegram.dto.TelegramFile;
import org.blacksoil.devcrew.notification.adapter.out.telegram.dto.TelegramUpdate;
import org.springframework.core.ParameterizedTypeReference;
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

  @Override
  public List<TelegramUpdate> getUpdates(long offset) {
    try {
      var response =
          restClient
              .get()
              .uri(API_URL + botToken + "/getUpdates?offset=" + offset + "&timeout=0")
              .retrieve()
              .body(new ParameterizedTypeReference<TelegramApiResponse<List<TelegramUpdate>>>() {});
      return response != null && response.ok() ? response.result() : List.of();
    } catch (Exception e) {
      log.error("Ошибка при получении обновлений Telegram", e);
      return List.of();
    }
  }

  @Override
  public byte[] downloadFile(String fileId) {
    try {
      var fileResponse =
          restClient
              .get()
              .uri(API_URL + botToken + "/getFile?file_id=" + fileId)
              .retrieve()
              .body(new ParameterizedTypeReference<TelegramApiResponse<TelegramFile>>() {});
      if (fileResponse == null || !fileResponse.ok() || fileResponse.result() == null) {
        return new byte[0];
      }
      var bytes =
          restClient
              .get()
              .uri(
                  "https://api.telegram.org/file/bot"
                      + botToken
                      + "/"
                      + fileResponse.result().filePath())
              .retrieve()
              .body(byte[].class);
      return bytes != null ? bytes : new byte[0];
    } catch (Exception e) {
      log.error("Ошибка при загрузке файла из Telegram: fileId={}", fileId, e);
      return new byte[0];
    }
  }
}
