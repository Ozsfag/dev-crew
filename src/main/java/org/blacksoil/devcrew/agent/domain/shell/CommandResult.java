package org.blacksoil.devcrew.agent.domain.shell;

/** Результат выполнения внешней команды. */
public record CommandResult(int exitCode, String output) {

  public boolean isSuccess() {
    return exitCode == 0;
  }
}
