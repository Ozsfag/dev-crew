package org.blacksoil.devcrew.agent.adapter.out.claude;

import com.fasterxml.jackson.annotation.JsonProperty;

/** JSON-ответ Claude Code CLI при запуске с --output-format json. */
public record ClaudeCodeOutput(
    String type,
    String result,
    @JsonProperty("num_turns") int numTurns,
    @JsonProperty("is_error") boolean isError) {}
