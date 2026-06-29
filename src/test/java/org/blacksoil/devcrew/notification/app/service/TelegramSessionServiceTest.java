package org.blacksoil.devcrew.notification.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.blacksoil.devcrew.agent.domain.ClaudeSessionRunner;
import org.blacksoil.devcrew.agent.domain.model.SessionResult;
import org.blacksoil.devcrew.common.TimeProvider;
import org.blacksoil.devcrew.notification.app.config.InteractiveProperties;
import org.blacksoil.devcrew.notification.domain.NotificationPort;
import org.blacksoil.devcrew.notification.domain.VoiceTranscriptionPort;
import org.blacksoil.devcrew.notification.domain.model.ChatSessionModel;
import org.blacksoil.devcrew.notification.domain.store.ChatSessionStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TelegramSessionServiceTest {

  private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
  private static final long CHAT = 100L;

  @Mock private ClaudeSessionRunner sessionRunner;
  @Mock private ChatSessionStore sessionStore;
  @Mock private NotificationPort notificationPort;
  @Mock private VoiceTranscriptionPort voiceTranscriptionPort;
  @Mock private TimeProvider timeProvider;

  private TelegramSessionService service;

  @BeforeEach
  void setUp() {
    var properties = new InteractiveProperties();
    properties.setProjects(
        Map.of("vpn-app", "/projects/vpn-app", "dev-crew", "/projects/dev-crew"));
    service =
        new TelegramSessionService(
            sessionRunner,
            sessionStore,
            notificationPort,
            voiceTranscriptionPort,
            properties,
            timeProvider,
            Runnable::run);
  }

  @Test
  void handleText_project_known_selects_and_saves_current() {
    when(timeProvider.now()).thenReturn(NOW);
    when(sessionStore.findByChatId(CHAT)).thenReturn(List.of());
    when(sessionStore.find(CHAT, "vpn-app")).thenReturn(Optional.empty());

    service.handleText(CHAT, "/project vpn-app");

    var captor = ArgumentCaptor.<ChatSessionModel>captor();
    verify(sessionStore).save(captor.capture());
    assertThat(captor.getValue().projectName()).isEqualTo("vpn-app");
    assertThat(captor.getValue().current()).isTrue();
    verify(notificationPort).send(contains("vpn-app"));
  }

  @Test
  void handleText_project_switch_deactivates_previous_current() {
    when(timeProvider.now()).thenReturn(NOW);
    var prev = session(UUID.randomUUID(), "dev-crew", "old-sess", true);
    when(sessionStore.findByChatId(CHAT)).thenReturn(List.of(prev));
    when(sessionStore.find(CHAT, "vpn-app")).thenReturn(Optional.empty());

    service.handleText(CHAT, "/project vpn-app");

    var captor = ArgumentCaptor.<ChatSessionModel>captor();
    verify(sessionStore, org.mockito.Mockito.times(2)).save(captor.capture());
    var saved = captor.getAllValues();
    assertThat(saved).anyMatch(s -> s.projectName().equals("dev-crew") && !s.current());
    assertThat(saved).anyMatch(s -> s.projectName().equals("vpn-app") && s.current());
  }

  @Test
  void handleText_project_unknown_sends_error() {
    service.handleText(CHAT, "/project nope");

    verify(notificationPort).send(contains("Неизвестный проект"));
    verify(sessionStore, never()).save(any());
  }

  @Test
  void handleText_project_without_name_sends_hint() {
    service.handleText(CHAT, "/project");

    verify(notificationPort).send(contains("Укажи имя проекта"));
    verify(sessionStore, never()).save(any());
  }

  @Test
  void handleText_projects_lists_available() {
    service.handleText(CHAT, "/projects");

    verify(notificationPort).send(contains("vpn-app"));
    verifyNoInteractions(sessionRunner);
  }

  @Test
  void handleText_new_clears_session_id_for_current_project() {
    when(timeProvider.now()).thenReturn(NOW);
    var current = session(UUID.randomUUID(), "vpn-app", "sess-1", true);
    when(sessionStore.findCurrent(CHAT)).thenReturn(Optional.of(current));

    service.handleText(CHAT, "/new");

    var captor = ArgumentCaptor.<ChatSessionModel>captor();
    verify(sessionStore).save(captor.capture());
    assertThat(captor.getValue().claudeSessionId()).isNull();
    verify(notificationPort).send(contains("новый диалог"));
  }

  @Test
  void handleText_new_without_project_sends_hint() {
    when(sessionStore.findCurrent(CHAT)).thenReturn(Optional.empty());

    service.handleText(CHAT, "/new");

    verify(notificationPort).send(contains("Сначала выбери проект"));
    verify(sessionStore, never()).save(any());
  }

  @Test
  void handleText_without_selected_project_sends_hint() {
    when(sessionStore.findCurrent(CHAT)).thenReturn(Optional.empty());

    service.handleText(CHAT, "поправь баг");

    verify(notificationPort).send(contains("Сначала выбери проект"));
    verifyNoInteractions(sessionRunner);
  }

  @Test
  void handleText_runs_session_without_prior_session_id() {
    when(timeProvider.now()).thenReturn(NOW);
    var current = session(UUID.randomUUID(), "vpn-app", null, true);
    when(sessionStore.findCurrent(CHAT)).thenReturn(Optional.of(current));
    when(sessionRunner.continueSession(eq("/projects/vpn-app"), isNull(), eq("сделай X")))
        .thenReturn(new SessionResult("сделано", "new-sess"));

    service.handleText(CHAT, "сделай X");

    verify(sessionRunner).continueSession("/projects/vpn-app", null, "сделай X");
    var captor = ArgumentCaptor.<ChatSessionModel>captor();
    verify(sessionStore).save(captor.capture());
    assertThat(captor.getValue().claudeSessionId()).isEqualTo("new-sess");
    verify(notificationPort).send("сделано");
  }

  @Test
  void handleText_resumes_existing_session_id() {
    when(timeProvider.now()).thenReturn(NOW);
    var current = session(UUID.randomUUID(), "vpn-app", "old-sess", true);
    when(sessionStore.findCurrent(CHAT)).thenReturn(Optional.of(current));
    when(sessionRunner.continueSession("/projects/vpn-app", "old-sess", "дальше"))
        .thenReturn(new SessionResult("ок", "old-sess"));

    service.handleText(CHAT, "дальше");

    verify(sessionRunner).continueSession("/projects/vpn-app", "old-sess", "дальше");
  }

  @Test
  void handleText_sends_error_when_project_path_not_configured() {
    var current = session(UUID.randomUUID(), "ghost", null, true);
    when(sessionStore.findCurrent(CHAT)).thenReturn(Optional.of(current));

    service.handleText(CHAT, "сделай X");

    verify(notificationPort).send(contains("больше не сконфигурирован"));
    verifyNoInteractions(sessionRunner);
  }

  @Test
  void handleText_sends_error_when_runner_throws() {
    var current = session(UUID.randomUUID(), "vpn-app", null, true);
    when(sessionStore.findCurrent(CHAT)).thenReturn(Optional.of(current));
    when(sessionRunner.continueSession(anyString(), any(), anyString()))
        .thenThrow(new RuntimeException("claude упал"));

    service.handleText(CHAT, "сделай X");

    verify(notificationPort).send(contains("Ошибка выполнения"));
  }

  @Test
  void handleVoice_transcribes_then_runs_session() {
    when(timeProvider.now()).thenReturn(NOW);
    var audio = new byte[] {1, 2, 3};
    when(voiceTranscriptionPort.transcribe(audio)).thenReturn("голосовая задача");
    var current = session(UUID.randomUUID(), "vpn-app", null, true);
    when(sessionStore.findCurrent(CHAT)).thenReturn(Optional.of(current));
    when(sessionRunner.continueSession(anyString(), isNull(), eq("голосовая задача")))
        .thenReturn(new SessionResult("готово", "s"));

    service.handleVoice(CHAT, audio);

    verify(sessionRunner).continueSession("/projects/vpn-app", null, "голосовая задача");
  }

  @Test
  void handleVoice_skips_when_transcription_empty() {
    var audio = new byte[] {1, 2, 3};
    when(voiceTranscriptionPort.transcribe(audio)).thenReturn("");

    service.handleVoice(CHAT, audio);

    verifyNoInteractions(sessionRunner);
    verify(notificationPort, never()).send(anyString());
  }

  private static ChatSessionModel session(
      UUID id, String project, String claudeSessionId, boolean current) {
    return new ChatSessionModel(id, CHAT, project, claudeSessionId, current, NOW, NOW);
  }
}
