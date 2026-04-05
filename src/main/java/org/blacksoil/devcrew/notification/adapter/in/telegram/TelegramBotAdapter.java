package org.blacksoil.devcrew.notification.adapter.in.telegram;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.blacksoil.devcrew.notification.adapter.out.telegram.TelegramApiClient;
import org.blacksoil.devcrew.notification.adapter.out.telegram.dto.TelegramUpdate;
import org.blacksoil.devcrew.notification.app.config.TelegramProperties;
import org.blacksoil.devcrew.notification.app.service.TelegramInboundService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Принимает входящие Telegram-сообщения через long-polling. Активируется только при
 * devcrew.notification.telegram.bot-enabled=true.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "devcrew.notification.telegram.bot-enabled", havingValue = "true")
public class TelegramBotAdapter {

  private final TelegramApiClient telegramApiClient;
  private final TelegramProperties telegramProperties;
  private final TelegramInboundService inboundService;

  private final AtomicLong offset = new AtomicLong(0);
  private final AtomicInteger consecutiveErrors = new AtomicInteger(0);

  @Scheduled(fixedDelayString = "${devcrew.notification.telegram.poll-delay-ms:1000}")
  public void pollUpdates() {
    try {
      var updates = telegramApiClient.getUpdates(offset.get());
      consecutiveErrors.set(0);
      for (var update : updates) {
        if (isAllowedChat(update)) {
          processUpdate(update);
        }
        offset.set(update.updateId() + 1);
      }
    } catch (Exception e) {
      int errors = consecutiveErrors.incrementAndGet();
      if (errors == 1 || errors % 10 == 0) {
        log.error("Ошибка Telegram polling ({}й раз подряд)", errors, e);
      }
    }
  }

  private void processUpdate(TelegramUpdate update) {
    var message = update.message();
    if (message == null || message.chat() == null) return;
    if (message.text() != null && !message.text().isBlank()) {
      inboundService.handleText(message.chat().id(), message.text());
    } else if (message.voice() != null) {
      var audioBytes = telegramApiClient.downloadFile(message.voice().fileId());
      inboundService.handleVoice(message.chat().id(), audioBytes);
    }
  }

  private boolean isAllowedChat(TelegramUpdate update) {
    var allowedChatId = telegramProperties.getAllowedChatId();
    if (allowedChatId == 0) return true;
    if (update.message() == null) return false;
    return update.message().chat().id() == allowedChatId;
  }
}
