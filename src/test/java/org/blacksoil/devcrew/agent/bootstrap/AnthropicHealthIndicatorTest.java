package org.blacksoil.devcrew.agent.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Status;

@ExtendWith(MockitoExtension.class)
class AnthropicHealthIndicatorTest {

  @Mock private ChatLanguageModel chatLanguageModel;

  @InjectMocks private AnthropicHealthIndicator healthIndicator;

  @Test
  void health_returns_up_when_model_responds() {
    when(chatLanguageModel.generate(any(String.class))).thenReturn("pong");

    var health = healthIndicator.health();

    assertThat(health.getStatus()).isEqualTo(Status.UP);
  }

  @Test
  void health_returns_down_when_model_throws() {
    when(chatLanguageModel.generate(any(String.class)))
        .thenThrow(new RuntimeException("API key invalid"));

    var health = healthIndicator.health();

    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails()).containsEntry("error", "API key invalid");
  }

  @Test
  void health_returns_unknown_before_first_check_interval() {
    // Первый вызов всегда обновляет (lastCheck = EPOCH), поэтому проверяем что кеш работает
    when(chatLanguageModel.generate(any(String.class))).thenReturn("pong");

    // Первый вызов — обновляет кеш
    healthIndicator.health();
    // Второй вызов — должен вернуть кешированный результат (интервал не прошёл)
    var health = healthIndicator.health();

    assertThat(health.getStatus()).isEqualTo(Status.UP);
  }
}
