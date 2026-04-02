package org.blacksoil.devcrew.agent.app;

import lombok.RequiredArgsConstructor;
import org.blacksoil.devcrew.agent.app.service.execution.AgentExecutionService;
import org.blacksoil.devcrew.agent.domain.AgentOrchestrator;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.task.app.service.command.TaskCommandService;
import org.blacksoil.devcrew.task.app.service.query.TaskQueryService;
import org.springframework.stereotype.Service;

import java.util.UUID;

import org.springframework.lang.Nullable;

/**
 * Оркестратор агентов: принимает задачу, создаёт её в хранилище, запускает исполнение.
 */
@Service
@RequiredArgsConstructor
public class AgentOrchestratorImpl implements AgentOrchestrator {

    private final TaskCommandService taskCommandService;
    private final TaskQueryService taskQueryService;
    private final AgentExecutionService agentExecutionService;

    @Override
    public UUID submit(String title, String description, AgentRole role, @Nullable UUID projectId) {
        var task = taskCommandService.create(title, description, role, projectId, null);
        return task.id();
    }

    @Override
    public void run(UUID taskId, AgentRole role) {
        var task = taskQueryService.getById(taskId);
        agentExecutionService.execute(taskId, role, task.description());
    }
}
