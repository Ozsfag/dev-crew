package org.blacksoil.devcrew.task.domain;

/**
 * Жизненный цикл задачи для агента.
 */
public enum TaskStatus {
    PENDING,
    IN_PROGRESS,
    WAITING_APPROVAL,
    APPROVED,
    REJECTED,
    COMPLETED,
    FAILED
}
