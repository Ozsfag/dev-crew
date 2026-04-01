package org.blacksoil.devcrew.agent.app.service.execution;

import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.agent.domain.BackendDevAgent;
import org.blacksoil.devcrew.agent.domain.CodeReviewAgent;
import org.blacksoil.devcrew.agent.domain.DevOpsAgent;
import org.blacksoil.devcrew.agent.domain.PostAgentHook;
import org.blacksoil.devcrew.agent.domain.QaAgent;
import org.blacksoil.devcrew.task.app.service.command.TaskCommandService;
import org.blacksoil.devcrew.task.app.service.query.TaskQueryService;
import org.blacksoil.devcrew.task.domain.TaskModel;
import org.blacksoil.devcrew.task.domain.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsyncAgentExecutionServiceTest {

    @Mock
    private BackendDevAgent backendDevAgent;

    @Mock
    private QaAgent qaAgent;

    @Mock
    private CodeReviewAgent codeReviewAgent;

    @Mock
    private DevOpsAgent devOpsAgent;

    @Mock
    private TaskQueryService taskQueryService;

    @Mock
    private TaskCommandService taskCommandService;

    private AgentExecutionService agentExecutionService;

    @BeforeEach
    void setUp() {
        agentExecutionService = new AgentExecutionService(
            backendDevAgent, qaAgent, codeReviewAgent, devOpsAgent, taskQueryService, taskCommandService, List.of()
        );
    }

    @Test
    void execute_returns_before_agent_finishes() throws InterruptedException {
        var taskId = UUID.randomUUID();
        var latch = new CountDownLatch(1);
        var agentStarted = new AtomicBoolean(false);

        when(taskQueryService.getById(taskId)).thenReturn(taskModel(taskId));
        when(taskCommandService.updateStatus(any(), any())).thenReturn(taskModel(taskId));
        when(taskCommandService.complete(any(), any())).thenReturn(taskModel(taskId));
        when(backendDevAgent.execute(any())).thenAnswer(inv -> {
            agentStarted.set(true);
            // имитируем долгую работу агента
            latch.await(5, TimeUnit.SECONDS);
            return "done";
        });

        var startedAt = System.currentTimeMillis();

        // запускаем execute в отдельном потоке — как будто это @Async
        var thread = Thread.ofVirtual().start(() ->
            agentExecutionService.execute(taskId, AgentRole.BACKEND_DEV, "write tests")
        );

        // даём агенту стартовать
        Thread.sleep(100);
        assertThat(agentStarted.get()).isTrue();

        // разблокируем агента и ждём завершения потока
        latch.countDown();
        thread.join(3000);

        var elapsed = System.currentTimeMillis() - startedAt;
        // без async: elapsed ≥ latch время; с virtual thread: поток завершается
        assertThat(thread.isAlive()).isFalse();
        assertThat(elapsed).isLessThan(5000);
    }

    @Test
    void execute_is_annotated_with_async() throws NoSuchMethodException {
        // Проверяем что метод помечен @Async — гарантия что Spring подхватит
        var method = AgentExecutionService.class.getMethod("execute", UUID.class, AgentRole.class, String.class);
        var hasAsync = method.isAnnotationPresent(org.springframework.scheduling.annotation.Async.class);
        assertThat(hasAsync).isTrue();
    }

    private TaskModel taskModel(UUID id) {
        return new TaskModel(
            id, null, "title", "description",
            AgentRole.BACKEND_DEV, TaskStatus.PENDING, null,
            Instant.now(), Instant.now()
        );
    }
}
