package org.blacksoil.devcrew.agent.app;

import org.blacksoil.devcrew.agent.app.service.execution.AgentExecutionService;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.task.app.service.command.TaskCommandService;
import org.blacksoil.devcrew.task.domain.TaskModel;
import org.blacksoil.devcrew.task.domain.TaskStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentOrchestratorImplTest {

    @Mock
    private TaskCommandService taskCommandService;

    @Mock
    private AgentExecutionService agentExecutionService;

    @Mock
    private org.blacksoil.devcrew.task.app.service.query.TaskQueryService taskQueryService;

    @InjectMocks
    private AgentOrchestratorImpl orchestrator;

    @Test
    void submit_creates_task_and_returns_its_id() {
        var expected = taskModel(UUID.randomUUID());
        when(taskCommandService.create(any(), any(), any(), any())).thenReturn(expected);

        var taskId = orchestrator.submit("Write tests", "TDD for UserService", AgentRole.BACKEND_DEV);

        assertThat(taskId).isEqualTo(expected.id());
    }

    @Test
    void submit_creates_task_with_correct_role_and_no_parent() {
        var expected = taskModel(UUID.randomUUID());
        when(taskCommandService.create(any(), any(), any(), any())).thenReturn(expected);

        orchestrator.submit("Write tests", "TDD for UserService", AgentRole.QA);

        var roleCaptor = ArgumentCaptor.<AgentRole>captor();
        verify(taskCommandService).create(
            eq("Write tests"), eq("TDD for UserService"),
            roleCaptor.capture(), eq(null)
        );
        assertThat(roleCaptor.getValue()).isEqualTo(AgentRole.QA);
    }

    @Test
    void run_fetches_task_description_and_delegates_to_execution_service() {
        var taskId = UUID.randomUUID();
        var task = taskModel(taskId);
        when(taskQueryService.getById(taskId)).thenReturn(task);

        orchestrator.run(taskId, AgentRole.BACKEND_DEV);

        verify(agentExecutionService).execute(taskId, AgentRole.BACKEND_DEV, task.description());
    }

    private TaskModel taskModel(UUID id) {
        return new TaskModel(
            id, null, "title", "description",
            AgentRole.BACKEND_DEV, TaskStatus.PENDING, null,
            Instant.now(), Instant.now()
        );
    }
}
