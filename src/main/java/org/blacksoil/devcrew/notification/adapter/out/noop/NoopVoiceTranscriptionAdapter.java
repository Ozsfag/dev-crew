package org.blacksoil.devcrew.notification.adapter.out.noop;

import lombok.extern.slf4j.Slf4j;
import org.blacksoil.devcrew.notification.domain.VoiceTranscriptionPort;
import org.springframework.stereotype.Component;

/**
 * Заглушка транскрипции голоса. Логирует предупреждение и возвращает пустую строку. Заменяется на
 * WhisperTranscriptionAdapter когда OPENAI_API_KEY будет настроен.
 */
@Slf4j
@Component
public class NoopVoiceTranscriptionAdapter implements VoiceTranscriptionPort {

  @Override
  public String transcribe(byte[] audioBytes) {
    log.warn(
        "Голосовая транскрипция не настроена (OPENAI_API_KEY отсутствует) — сообщение проигнорировано");
    return "";
  }
}
