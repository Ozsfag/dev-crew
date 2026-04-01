package org.blacksoil.devcrew.agent.adapter.out.llm.process;

/**
 * Результат выполнения внешней команды.
 */
public record CommandResult(int exitCode, String output) {

    public boolean isSuccess() {
        return exitCode == 0;
    }
}
