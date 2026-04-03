package org.blacksoil.devcrew.notification.bootstrap;

import org.blacksoil.devcrew.notification.adapter.out.telegram.TelegramApiClient;
import org.blacksoil.devcrew.notification.adapter.out.telegram.TelegramApiClientImpl;
import org.blacksoil.devcrew.notification.app.config.TelegramProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** Создаёт Spring-бин TelegramApiClient. Вынесен в bootstrap/, т.к. зависит от adapter/out/. */
@Configuration
public class TelegramClientConfig {

  @Bean
  public TelegramApiClient telegramApiClient(TelegramProperties properties) {
    return new TelegramApiClientImpl(RestClient.create(), properties.getBotToken());
  }
}
