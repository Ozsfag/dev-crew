package org.blacksoil.devcrew.notification.domain;

/** Port для транскрипции голосовых сообщений в текст. */
public interface VoiceTranscriptionPort {

  /**
   * Транскрибирует аудио в текст.
   *
   * @param audioBytes аудио-данные (ogg/mp3)
   * @return распознанный текст или пустая строка если транскрипция недоступна
   */
  String transcribe(byte[] audioBytes);
}
