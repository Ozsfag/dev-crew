package org.blacksoil.devcrew.notification.app.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TelegramBotValidator implements InitializingBean {

  private final TelegramProperties properties;

  @Override
  public void afterPropertiesSet() {
    if (properties.isBotEnabled() && properties.getAllowedChatId() == 0L) {
      throw new IllegalStateException(
          "devcrew.notification.telegram.allowed-chat-id обязателен при botEnabled=true");
    }
  }
}
