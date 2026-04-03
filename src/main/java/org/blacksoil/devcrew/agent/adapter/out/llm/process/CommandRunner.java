package org.blacksoil.devcrew.agent.adapter.out.llm.process;

import java.io.File;

/**
 * Port для запуска внешних команд. Изолирует ProcessBuilder от бизнес-логики, позволяет подменять
 * реализацию в тестах.
 */
public interface CommandRunner {

  /**
   * Запускает команду в указанной директории и возвращает объединённый stdout+stderr.
   *
   * @param workDir рабочая директория
   * @param command команда и аргументы
   * @return вывод процесса
   */
  CommandResult run(File workDir, String... command);
}
