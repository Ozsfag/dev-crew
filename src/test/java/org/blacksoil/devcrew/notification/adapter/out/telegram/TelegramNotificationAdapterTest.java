package org.blacksoil.devcrew.notification.adapter.out.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.UUID;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.notification.app.config.TelegramProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TelegramNotificationAdapterTest {

  @Mock private TelegramApiClient telegramApiClient;

  @Mock private TelegramProperties telegramProperties;

  @InjectMocks private TelegramNotificationAdapter adapter;

  @Test
  void send_calls_api_client_when_enabled() {
    var chatId = "123456";
    org.mockito.Mockito.when(telegramProperties.isEnabled()).thenReturn(true);
    org.mockito.Mockito.when(telegramProperties.getChatId()).thenReturn(chatId);
    org.mockito.Mockito.when(telegramProperties.getMaxMessageLength()).thenReturn(4096);

    adapter.send("Hello architect");

    var captor = ArgumentCaptor.<String>captor();
    verify(telegramApiClient)
        .sendMessage(org.mockito.ArgumentMatchers.eq(chatId), captor.capture());
    assertThat(captor.getValue()).isEqualTo("Hello architect");
  }

  @Test
  void send_skips_api_call_when_disabled() {
    org.mockito.Mockito.when(telegramProperties.isEnabled()).thenReturn(false);

    adapter.send("Hello architect");

    verifyNoInteractions(telegramApiClient);
  }

  @Test
  void onAgentCompleted_sends_formatted_message() {
    var taskId = UUID.randomUUID();
    org.mockito.Mockito.when(telegramProperties.isEnabled()).thenReturn(true);
    org.mockito.Mockito.when(telegramProperties.getChatId()).thenReturn("999");
    org.mockito.Mockito.when(telegramProperties.getMaxMessageLength()).thenReturn(4096);

    adapter.onAgentCompleted(taskId, null, AgentRole.BACKEND_DEV, "Tests written: FooTest.java");

    var captor = ArgumentCaptor.<String>captor();
    verify(telegramApiClient).sendMessage(org.mockito.ArgumentMatchers.eq("999"), captor.capture());
    var message = captor.getValue();
    assertThat(message).contains("BACKEND_DEV");
    assertThat(message).contains(taskId.toString());
    assertThat(message).contains("Tests written: FooTest.java");
  }

  @Test
  void onAgentCompleted_skips_when_disabled() {
    org.mockito.Mockito.when(telegramProperties.isEnabled()).thenReturn(false);

    adapter.onAgentCompleted(UUID.randomUUID(), null, AgentRole.QA, "result");

    verifyNoInteractions(telegramApiClient);
  }

  @Test
  void send_truncates_long_message_to_telegram_limit() {
    org.mockito.Mockito.when(telegramProperties.isEnabled()).thenReturn(true);
    org.mockito.Mockito.when(telegramProperties.getChatId()).thenReturn("1");
    org.mockito.Mockito.when(telegramProperties.getMaxMessageLength()).thenReturn(4096);

    // Telegram лимит — 4096 символов
    var longMessage = "x".repeat(5000);
    adapter.send(longMessage);

    var captor = ArgumentCaptor.<String>captor();
    verify(telegramApiClient).sendMessage(org.mockito.ArgumentMatchers.any(), captor.capture());
    assertThat(captor.getValue().length()).isLessThanOrEqualTo(4096);
  }
}
