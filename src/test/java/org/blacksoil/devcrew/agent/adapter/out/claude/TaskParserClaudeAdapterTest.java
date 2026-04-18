package org.blacksoil.devcrew.agent.adapter.out.claude;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.agent.domain.ClaudeCodeRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskParserClaudeAdapterTest {

  @Mock private ClaudeCodeRunner claudeCodeRunner;

  private TaskParserClaudeAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new TaskParserClaudeAdapter(claudeCodeRunner, new ObjectMapper());
  }

  @Test
  void parse_returns_parsed_task_from_valid_json() {
    var json =
        "{\"title\":\"Fix NPE\",\"description\":\"Fix null pointer\",\"agentRole\":\"BACKEND_DEV\"}";
    when(claudeCodeRunner.run(anyString(), eq("Fix the bug"))).thenReturn(json);

    var result = adapter.parse("Fix the bug");

    assertThat(result.title()).isEqualTo("Fix NPE");
    assertThat(result.description()).isEqualTo("Fix null pointer");
    assertThat(result.agentRole()).isEqualTo(AgentRole.BACKEND_DEV);
  }

  @Test
  void parse_passes_task_parser_system_prompt_to_runner() {
    var json = "{\"title\":\"t\",\"description\":\"d\",\"agentRole\":\"BACKEND_DEV\"}";
    when(claudeCodeRunner.run(anyString(), eq("some message"))).thenReturn(json);

    adapter.parse("some message");

    // Системный промпт загружается из prompts/task-parser.md
    verify(claudeCodeRunner).run(contains("task parser"), eq("some message"));
  }

  @Test
  void parse_returns_fallback_on_invalid_json() {
    when(claudeCodeRunner.run(anyString(), eq("some message"))).thenReturn("not valid json");

    var result = adapter.parse("some message");

    assertThat(result.title()).isBlank();
    assertThat(result.description()).isEqualTo("some message");
    assertThat(result.agentRole()).isEqualTo(AgentRole.BACKEND_DEV);
  }

  @Test
  void parse_returns_fallback_when_runner_throws() {
    when(claudeCodeRunner.run(anyString(), anyString()))
        .thenThrow(new RuntimeException("claude CLI error"));

    var result = adapter.parse("task");

    assertThat(result.title()).isBlank();
  }
}
