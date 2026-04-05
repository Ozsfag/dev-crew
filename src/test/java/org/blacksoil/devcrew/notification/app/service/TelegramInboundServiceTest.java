package org.blacksoil.devcrew.notification.app.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.blacksoil.devcrew.agent.domain.AgentOrchestrator;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.agent.domain.TaskParsingPort;
import org.blacksoil.devcrew.agent.domain.model.ParsedTask;
import org.blacksoil.devcrew.notification.domain.NotificationPort;
import org.blacksoil.devcrew.notification.domain.VoiceTranscriptionPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TelegramInboundServiceTest {

  @Mock private VoiceTranscriptionPort voiceTranscriptionPort;
  @Mock private TaskParsingPort taskParsingPort;
  @Mock private AgentOrchestrator agentOrchestrator;
  @Mock private NotificationPort notificationPort;

  private TelegramInboundService service;

  @BeforeEach
  void setUp() {
    service =
        new TelegramInboundService(
            voiceTranscriptionPort, taskParsingPort, agentOrchestrator, notificationPort);
  }

  @Test
  void handleText_submits_task_and_runs_agent() {
    var taskId = UUID.randomUUID();
    var parsed = new ParsedTask("Fix bug", "Fix the NPE in UserService", AgentRole.BACKEND_DEV);
    when(taskParsingPort.parse("Fix the bug")).thenReturn(parsed);
    when(agentOrchestrator.submit(
            "Fix bug", "Fix the NPE in UserService", AgentRole.BACKEND_DEV, null))
        .thenReturn(taskId);

    service.handleText(123L, "Fix the bug");

    verify(agentOrchestrator)
        .submit("Fix bug", "Fix the NPE in UserService", AgentRole.BACKEND_DEV, null);
    verify(agentOrchestrator).run(taskId, AgentRole.BACKEND_DEV);
    verify(notificationPort).send(contains(taskId.toString()));
  }

  @Test
  void handleText_sends_error_when_parsed_title_is_blank() {
    var parsed = new ParsedTask("", "some text", AgentRole.BACKEND_DEV);
    when(taskParsingPort.parse(anyString())).thenReturn(parsed);

    service.handleText(123L, "непонятный текст");

    verifyNoInteractions(agentOrchestrator);
    verify(notificationPort).send(contains("Не удалось распознать"));
  }

  @Test
  void handleVoice_transcribes_and_submits_task() {
    var audioBytes = new byte[] {1, 2, 3};
    var taskId = UUID.randomUUID();
    var parsed = new ParsedTask("Write tests", "Write unit tests", AgentRole.QA);
    when(voiceTranscriptionPort.transcribe(audioBytes)).thenReturn("Write unit tests for me");
    when(taskParsingPort.parse("Write unit tests for me")).thenReturn(parsed);
    when(agentOrchestrator.submit(any(), any(), any(), any())).thenReturn(taskId);

    service.handleVoice(123L, audioBytes);

    verify(voiceTranscriptionPort).transcribe(audioBytes);
    verify(agentOrchestrator).submit("Write tests", "Write unit tests", AgentRole.QA, null);
  }

  @Test
  void handleVoice_skips_when_transcription_is_empty() {
    var audioBytes = new byte[] {1, 2, 3};
    when(voiceTranscriptionPort.transcribe(audioBytes)).thenReturn("");

    service.handleVoice(123L, audioBytes);

    verifyNoInteractions(agentOrchestrator);
    verifyNoInteractions(notificationPort);
    verifyNoInteractions(taskParsingPort);
  }
}
