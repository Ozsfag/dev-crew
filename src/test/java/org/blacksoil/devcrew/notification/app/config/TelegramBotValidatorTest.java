package org.blacksoil.devcrew.notification.app.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TelegramBotValidatorTest {

  @Test
  void afterPropertiesSet_ok_when_botDisabled() {
    var props = properties(false, 0L);
    assertThatCode(() -> new TelegramBotValidator(props).afterPropertiesSet())
        .doesNotThrowAnyException();
  }

  @Test
  void afterPropertiesSet_ok_when_botEnabled_and_chatId_set() {
    var props = properties(true, 12345L);
    assertThatCode(() -> new TelegramBotValidator(props).afterPropertiesSet())
        .doesNotThrowAnyException();
  }

  @Test
  void afterPropertiesSet_throws_when_botEnabled_and_chatId_zero() {
    var props = properties(true, 0L);
    assertThatThrownBy(() -> new TelegramBotValidator(props).afterPropertiesSet())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("allowed-chat-id");
  }

  private static TelegramProperties properties(boolean botEnabled, long allowedChatId) {
    var props = new TelegramProperties();
    props.setBotEnabled(botEnabled);
    props.setAllowedChatId(allowedChatId);
    return props;
  }
}
