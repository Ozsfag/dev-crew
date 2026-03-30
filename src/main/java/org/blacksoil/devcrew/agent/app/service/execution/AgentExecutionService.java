package org.blacksoil.devcrew.agent.app.service.execution;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.blacksoil.devcrew.agent.domain.BackendDevAgent;
import org.blacksoil.devcrew.agent.domain.PostAgentHook;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.task.app.service.command.TaskCommandService;
import org.blacksoil.devcrew.task.app.service.query.TaskQueryService;
import org.blacksoil.devcrew.task.domain.TaskStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Запускает агента на задачу: переводит статус → вызывает агента → фиксирует результат → хуки.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentExecutionService {

    private final BackendDevAgent backendDevAgent;
    private final TaskQueryService taskQueryService;
    private final TaskCommandService taskCommandService;
    private final List<PostAgentHook> postAgentHooks;

    public void execute(UUID taskId, AgentRole role, String prompt) {
        taskQueryService.getById(taskId);
        taskCommandService.updateStatus(taskId, TaskStatus.IN_PROGRESS);

        String result;
        try {
            result = dispatchToAgent(role, prompt);
        } catch (Exception e) {
            log.error("Агент {} упал при выполнении задачи {}: {}", role, taskId, e.getMessage());
            taskCommandService.fail(taskId, e.getMessage());
            return;
        }

        taskCommandService.complete(taskId, result);
        notifyHooks(taskId, role, result);
    }

    private String dispatchToAgent(AgentRole role, String prompt) {
        // пока поддерживается только BACKEND_DEV; остальные роли — следующие шаги
        return switch (role) {
            case BACKEND_DEV -> backendDevAgent.execute(prompt);
            default -> throw new UnsupportedOperationException("Агент " + role + " ещё не реализован");
        };
    }

    private void notifyHooks(UUID taskId, AgentRole role, String result) {
        postAgentHooks.forEach(hook -> hook.onAgentCompleted(taskId, role, result));
    }
}
