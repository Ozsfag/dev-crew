package org.blacksoil.devcrew.notification.adapter.out.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.agent.domain.PostAgentHook;
import org.blacksoil.devcrew.notification.domain.NotificationPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Реализует NotificationPort и PostAgentHook.
 * Отправляет сообщения архитектору в Telegram после завершения агента.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramNotificationAdapter implements NotificationPort, PostAgentHook {

    private static final int TELEGRAM_MAX_LENGTH = 4096;

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
    public void onAgentCompleted(UUID taskId, AgentRole role, String result) {
        var message = """
            [Dev Crew] Агент завершил задачу
            Роль: %s
            Задача: %s
            Результат:
            %s
            """.formatted(role, taskId, result);
        send(message);
    }

    private String truncate(String message) {
        if (message.length() <= TELEGRAM_MAX_LENGTH) {
            return message;
        }
        return message.substring(0, TELEGRAM_MAX_LENGTH - 3) + "...";
    }
}
