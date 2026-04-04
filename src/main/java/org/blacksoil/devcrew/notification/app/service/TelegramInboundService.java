package org.blacksoil.devcrew.notification.app.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.blacksoil.devcrew.agent.domain.AgentOrchestrator;
import org.blacksoil.devcrew.notification.domain.NotificationPort;
import org.blacksoil.devcrew.notification.domain.VoiceTranscriptionPort;
import org.springframework.stereotype.Service;

/** Обрабатывает входящие Telegram-сообщения: парсит задачу и запускает агента. */
@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramInboundService {

  private final VoiceTranscriptionPort voiceTranscriptionPort;
  private final TaskParserService taskParserService;
  private final AgentOrchestrator agentOrchestrator;
  private final NotificationPort notificationPort;

  /** Обрабатывает текстовое сообщение: парсит в задачу и запускает агента. */
  public void handleText(long chatId, String text) {
    log.info("Telegram входящее сообщение: chatId={}", chatId);
    var parsed = taskParserService.parse(text);
    if (parsed.title().isBlank()) {
      notificationPort.send("Не удалось распознать задачу. Пожалуйста, опишите задачу подробнее.");
      return;
    }
    var taskId =
        agentOrchestrator.submit(parsed.title(), parsed.description(), parsed.agentRole(), null);
    agentOrchestrator.run(taskId, parsed.agentRole());
    notificationPort.send(
        "Задача принята: " + taskId + "\nАгент " + parsed.agentRole() + " приступил к работе.");
  }

  /** Обрабатывает голосовое сообщение: транскрибирует и обрабатывает как текст. */
  public void handleVoice(long chatId, byte[] audioBytes) {
    log.info("Telegram входящее голосовое сообщение: chatId={}", chatId);
    var text = voiceTranscriptionPort.transcribe(audioBytes);
    if (text.isBlank()) {
      log.debug("Транскрипция вернула пустую строку — пропускаем");
      return;
    }
    handleText(chatId, text);
  }
}
