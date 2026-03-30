package org.blacksoil.devcrew.agent.domain;

/**
 * Текущий статус агента.
 */
public enum AgentStatus {
    IDLE,
    RUNNING,
    WAITING_FOR_APPROVAL,
    COMPLETED,
    FAILED
}
