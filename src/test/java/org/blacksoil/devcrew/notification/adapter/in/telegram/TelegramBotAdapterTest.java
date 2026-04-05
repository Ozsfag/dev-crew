package org.blacksoil.devcrew.notification.adapter.in.telegram;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.blacksoil.devcrew.notification.adapter.out.telegram.TelegramApiClient;
import org.blacksoil.devcrew.notification.adapter.out.telegram.dto.TelegramChat;
import org.blacksoil.devcrew.notification.adapter.out.telegram.dto.TelegramMessage;
import org.blacksoil.devcrew.notification.adapter.out.telegram.dto.TelegramUpdate;
import org.blacksoil.devcrew.notification.adapter.out.telegram.dto.TelegramVoice;
import org.blacksoil.devcrew.notification.app.config.TelegramProperties;
import org.blacksoil.devcrew.notification.app.service.TelegramInboundService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TelegramBotAdapterTest {

  @Mock private TelegramApiClient telegramApiClient;
  @Mock private TelegramInboundService inboundService;

  private TelegramBotAdapter adapter;
  private TelegramProperties properties;

  @BeforeEach
  void setUp() {
    properties = new TelegramProperties();
    adapter = new TelegramBotAdapter(telegramApiClient, properties, inboundService);
  }

  @Test
  void pollUpdates_processes_text_message() {
    var update = update(1L, 100L, "напиши тесты", null);
    when(telegramApiClient.getUpdates(0L)).thenReturn(List.of(update));

    adapter.pollUpdates();

    verify(inboundService).handleText(100L, "напиши тесты");
  }

  @Test
  void pollUpdates_processes_voice_message_downloads_and_calls_handleVoice() {
    var voice = new TelegramVoice("file_id_123");
    var update = update(2L, 100L, null, voice);
    var audioBytes = new byte[] {1, 2, 3};
    when(telegramApiClient.getUpdates(0L)).thenReturn(List.of(update));
    when(telegramApiClient.downloadFile("file_id_123")).thenReturn(audioBytes);

    adapter.pollUpdates();

    verify(telegramApiClient).downloadFile("file_id_123");
    verify(inboundService).handleVoice(100L, audioBytes);
  }

  @Test
  void pollUpdates_skips_message_from_disallowed_chat() {
    properties.setAllowedChatId(999L);
    var update = update(3L, 100L, "text", null);
    when(telegramApiClient.getUpdates(0L)).thenReturn(List.of(update));

    adapter.pollUpdates();

    verify(inboundService, never()).handleText(anyLong(), anyString());
  }

  @Test
  void pollUpdates_allows_any_chat_when_allowedChatId_is_zero() {
    properties.setAllowedChatId(0L);
    var update = update(4L, 42L, "hello", null);
    when(telegramApiClient.getUpdates(0L)).thenReturn(List.of(update));

    adapter.pollUpdates();

    verify(inboundService).handleText(42L, "hello");
  }

  @Test
  void pollUpdates_advances_offset_even_for_disallowed_chat() {
    properties.setAllowedChatId(999L);
    var update = update(10L, 100L, "text", null);
    when(telegramApiClient.getUpdates(0L)).thenReturn(List.of(update));
    when(telegramApiClient.getUpdates(11L)).thenReturn(List.of());

    adapter.pollUpdates();
    adapter.pollUpdates();

    verify(telegramApiClient).getUpdates(11L);
  }

  @Test
  void pollUpdates_skips_update_with_null_message() {
    var update = new TelegramUpdate(5L, null);
    when(telegramApiClient.getUpdates(0L)).thenReturn(List.of(update));

    adapter.pollUpdates();

    verify(inboundService, never()).handleText(anyLong(), any());
    verify(inboundService, never()).handleVoice(anyLong(), any());
  }

  @Test
  void pollUpdates_skips_message_with_null_chat() {
    var message = new TelegramMessage(null, "hello", null);
    var update = new TelegramUpdate(6L, message);
    when(telegramApiClient.getUpdates(0L)).thenReturn(List.of(update));

    adapter.pollUpdates();

    verify(inboundService, never()).handleText(anyLong(), any());
    verify(inboundService, never()).handleVoice(anyLong(), any());
  }

  @Test
  void pollUpdates_does_not_throw_when_api_client_fails() {
    when(telegramApiClient.getUpdates(0L)).thenThrow(new RuntimeException("network error"));

    assertThatNoException().isThrownBy(() -> adapter.pollUpdates());
  }

  @Test
  void pollUpdates_resets_error_counter_after_successful_call() {
    when(telegramApiClient.getUpdates(0L))
        .thenThrow(new RuntimeException("fail"))
        .thenReturn(List.of())
        .thenThrow(new RuntimeException("fail again"));

    adapter.pollUpdates(); // error 1
    adapter.pollUpdates(); // success — resets counter
    adapter.pollUpdates(); // error 1 again (counter reset)

    // после сброса ошибка снова логируется (1й раз); 2 раза из 3 вызовов бросали исключение
    verify(telegramApiClient, times(3)).getUpdates(0L);
  }

  private TelegramUpdate update(long updateId, long chatId, String text, TelegramVoice voice) {
    var chat = new TelegramChat(chatId);
    var message = new TelegramMessage(chat, text, voice);
    return new TelegramUpdate(updateId, message);
  }
}
