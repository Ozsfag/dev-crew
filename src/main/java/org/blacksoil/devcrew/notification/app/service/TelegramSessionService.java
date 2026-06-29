package org.blacksoil.devcrew.notification.app.service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.blacksoil.devcrew.agent.domain.ClaudeSessionRunner;
import org.blacksoil.devcrew.common.TimeProvider;
import org.blacksoil.devcrew.notification.app.config.InteractiveProperties;
import org.blacksoil.devcrew.notification.domain.NotificationPort;
import org.blacksoil.devcrew.notification.domain.VoiceTranscriptionPort;
import org.blacksoil.devcrew.notification.domain.model.ChatSessionModel;
import org.blacksoil.devcrew.notification.domain.store.ChatSessionStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Обрабатывает входящие Telegram-сообщения как диалог с claude в папке выбранного проекта («Claude
 * Code через Telegram»). Команды: /project &lt;имя&gt; — выбрать проект, /projects — список, /new —
 * начать новый диалог. Обычный текст продолжает сеанс claude через --resume.
 *
 * <p>Долгий запуск claude выполняется на agentExecutor (virtual thread), чтобы не блокировать
 * Telegram long-polling. Пользователю сразу уходит ack, результат — по готовности.
 */
@Slf4j
@Service
public class TelegramSessionService {

  private final ClaudeSessionRunner sessionRunner;
  private final ChatSessionStore sessionStore;
  private final NotificationPort notificationPort;
  private final VoiceTranscriptionPort voiceTranscriptionPort;
  private final InteractiveProperties properties;
  private final TimeProvider timeProvider;
  private final Executor executor;

  public TelegramSessionService(
      ClaudeSessionRunner sessionRunner,
      ChatSessionStore sessionStore,
      NotificationPort notificationPort,
      VoiceTranscriptionPort voiceTranscriptionPort,
      InteractiveProperties properties,
      TimeProvider timeProvider,
      @Qualifier("agentExecutor") Executor executor) {
    this.sessionRunner = sessionRunner;
    this.sessionStore = sessionStore;
    this.notificationPort = notificationPort;
    this.voiceTranscriptionPort = voiceTranscriptionPort;
    this.properties = properties;
    this.timeProvider = timeProvider;
    this.executor = executor;
  }

  /** Обрабатывает текстовое сообщение: команда или продолжение сеанса claude. */
  public void handleText(long chatId, String text) {
    var trimmed = text.strip();
    if (trimmed.startsWith("/project ") || trimmed.equals("/project")) {
      selectProject(chatId, trimmed);
    } else if (trimmed.equals("/projects")) {
      notificationPort.send("Доступные проекты: " + availableProjects());
    } else if (trimmed.equals("/new")) {
      newSession(chatId);
    } else {
      runInSession(chatId, trimmed);
    }
  }

  /** Обрабатывает голосовое сообщение: транскрибирует и обрабатывает как текст. */
  public void handleVoice(long chatId, byte[] audioBytes) {
    var text = voiceTranscriptionPort.transcribe(audioBytes);
    if (text.isBlank()) {
      log.debug("Транскрипция вернула пустую строку — пропускаем");
      return;
    }
    handleText(chatId, text);
  }

  private void selectProject(long chatId, String command) {
    var parts = command.split("\\s+", 2);
    if (parts.length < 2 || parts[1].isBlank()) {
      notificationPort.send("Укажи имя проекта: /project <имя>. Доступно: " + availableProjects());
      return;
    }
    var name = parts[1].strip();
    if (!properties.getProjects().containsKey(name)) {
      notificationPort.send("Неизвестный проект «" + name + "». Доступно: " + availableProjects());
      return;
    }
    var now = timeProvider.now();
    // Снимаем флаг активного проекта со всех остальных проектов чата
    sessionStore.findByChatId(chatId).stream()
        .filter(ChatSessionModel::current)
        .filter(s -> !s.projectName().equals(name))
        .forEach(s -> sessionStore.save(withCurrent(s, false, now)));
    // Создаём или активируем выбранный проект (существующий сеанс сохраняем)
    var target =
        sessionStore
            .find(chatId, name)
            .map(s -> withCurrent(s, true, now))
            .orElseGet(
                () -> new ChatSessionModel(UUID.randomUUID(), chatId, name, null, true, now, now));
    sessionStore.save(target);
    notificationPort.send("Проект «" + name + "» выбран. Пиши задачу — буду работать в нём.");
  }

  private void newSession(long chatId) {
    var current = sessionStore.findCurrent(chatId);
    if (current.isEmpty()) {
      sendSelectProjectHint();
      return;
    }
    var s = current.get();
    sessionStore.save(withSessionId(s, null, timeProvider.now()));
    notificationPort.send("Начинаю новый диалог в проекте «" + s.projectName() + "».");
  }

  private void runInSession(long chatId, String message) {
    var current = sessionStore.findCurrent(chatId);
    if (current.isEmpty()) {
      sendSelectProjectHint();
      return;
    }
    var session = current.get();
    var repoPath = properties.getProjects().get(session.projectName());
    if (repoPath == null) {
      notificationPort.send("Проект «" + session.projectName() + "» больше не сконфигурирован.");
      return;
    }
    notificationPort.send("🤔 Работаю над задачей в проекте «" + session.projectName() + "»…");
    // Долгий запуск — на отдельном virtual thread, чтобы не блокировать polling
    executor.execute(() -> execute(session, repoPath, message));
  }

  private void execute(ChatSessionModel session, String repoPath, String message) {
    try {
      var result = sessionRunner.continueSession(repoPath, session.claudeSessionId(), message);
      sessionStore.save(withSessionId(session, result.sessionId(), timeProvider.now()));
      notificationPort.send(result.result());
    } catch (Exception e) {
      log.error("Ошибка выполнения сеанса claude: chatId={}", session.chatId(), e);
      notificationPort.send("⚠️ Ошибка выполнения: " + e.getMessage());
    }
  }

  private void sendSelectProjectHint() {
    notificationPort.send(
        "Сначала выбери проект: /project <имя>. Доступно: " + availableProjects());
  }

  private String availableProjects() {
    var names = properties.getProjects().keySet();
    return names.isEmpty() ? "(не настроены)" : String.join(", ", names);
  }

  /** Копия сессии с новым флагом активности (sessionId сохраняется). */
  private static ChatSessionModel withCurrent(ChatSessionModel s, boolean current, Instant now) {
    return new ChatSessionModel(
        s.id(), s.chatId(), s.projectName(), s.claudeSessionId(), current, s.createdAt(), now);
  }

  /** Копия сессии с новым id сеанса claude (флаг активности сохраняется). */
  private static ChatSessionModel withSessionId(ChatSessionModel s, String sessionId, Instant now) {
    return new ChatSessionModel(
        s.id(), s.chatId(), s.projectName(), sessionId, s.current(), s.createdAt(), now);
  }
}
