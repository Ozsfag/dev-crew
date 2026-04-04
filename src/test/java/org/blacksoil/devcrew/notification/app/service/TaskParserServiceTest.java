package org.blacksoil.devcrew.notification.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.agent.domain.agent.TaskParserAgent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskParserServiceTest {

  private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

  @Mock private TaskParserAgent taskParserAgent;

  private TaskParserService service;

  @BeforeEach
  void setUp() {
    service = new TaskParserService(taskParserAgent, new ObjectMapper());
  }

  @Test
  void parse_returns_parsed_task_from_valid_json() {
    var json =
        "{\"title\":\"Fix NPE\",\"description\":\"Fix null pointer in UserService\",\"agentRole\":\"BACKEND_DEV\"}";
    when(taskParserAgent.parse("Fix the bug")).thenReturn(json);

    var result = service.parse("Fix the bug");

    assertThat(result.title()).isEqualTo("Fix NPE");
    assertThat(result.description()).isEqualTo("Fix null pointer in UserService");
    assertThat(result.agentRole()).isEqualTo(AgentRole.BACKEND_DEV);
  }

  @Test
  void parse_returns_empty_title_on_invalid_json() {
    when(taskParserAgent.parse("some message")).thenReturn("not valid json at all");

    var result = service.parse("some message");

    assertThat(result.title()).isBlank();
    assertThat(result.description()).isEqualTo("some message");
    assertThat(result.agentRole()).isEqualTo(AgentRole.BACKEND_DEV);
  }
}
