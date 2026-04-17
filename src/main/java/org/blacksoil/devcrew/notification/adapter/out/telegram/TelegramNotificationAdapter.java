package org.blacksoil.devcrew.notification.adapter.out.telegram;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.agent.domain.hook.PostAgentHook;
import org.blacksoil.devcrew.notification.app.config.TelegramProperties;
import org.blacksoil.devcrew.notification.domain.NotificationPort;
import org.springframework.stereotype.Component;

/**
 * Реализует NotificationPort и PostAgentHook. Отправляет сообщения архитектору в Telegram после
 * завершения агента.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramNotificationAdapter implements NotificationPort, PostAgentHook {

  private final TelegramApiClient telegramApiClient;
  private final TelegramProperties telegramProperties;

  @Override
  public void send(String message) {
    if (!telegramProperties.isEnabled()) {
      log.debug("Telegram уведомления отключены, сообщение пропущено");
      return;
    }
    telegramApiClient.sendMessage(telegramProperties.getChatId(), truncate(message));
  }

  @Override
  public void onAgentCompleted(
      UUID taskId, UUID projectId, UUID orgId, AgentRole role, String result) {
    var message =
        """
            [Dev Crew] Агент завершил задачу
            Роль: %s
            Задача: %s
            Результат:
            %s
            """
            .formatted(role, taskId, result);
    send(message);
  }

  private String truncate(String message) {
    var maxLen = telegramProperties.getMaxMessageLength();
    if (message.length() <= maxLen) {
      return message;
    }
    return message.substring(0, maxLen - 3) + "...";
  }
}
