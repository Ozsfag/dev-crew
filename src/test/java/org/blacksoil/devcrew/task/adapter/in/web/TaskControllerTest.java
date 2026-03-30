package org.blacksoil.devcrew.task.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.blacksoil.devcrew.agent.domain.AgentOrchestrator;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.common.exception.NotFoundException;
import org.blacksoil.devcrew.common.web.GlobalExceptionHandler;
import org.blacksoil.devcrew.task.app.service.query.TaskQueryService;
import org.blacksoil.devcrew.task.domain.TaskModel;
import org.blacksoil.devcrew.task.domain.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TaskControllerTest {

    @Mock
    private TaskQueryService taskQueryService;

    @Mock
    private AgentOrchestrator agentOrchestrator;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        var controller = new TaskController(
            taskQueryService, agentOrchestrator, Mappers.getMapper(TaskWebMapper.class)
        );
        mockMvc = MockMvcBuilders
            .standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void POST_tasks_returns_201_with_task_id() throws Exception {
        var taskId = UUID.randomUUID();
        when(agentOrchestrator.submit(any(), any(), any())).thenReturn(taskId);

        var body = """
            {"title":"Write tests","description":"TDD for UserService","role":"BACKEND_DEV"}
            """;

        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.taskId").value(taskId.toString()));
    }

    @Test
    void POST_tasks_delegates_to_orchestrator_with_correct_params() throws Exception {
        when(agentOrchestrator.submit(any(), any(), any())).thenReturn(UUID.randomUUID());

        var body = """
            {"title":"Write tests","description":"TDD","role":"QA"}
            """;

        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated());

        verify(agentOrchestrator).submit("Write tests", "TDD", AgentRole.QA);
    }

    @Test
    void POST_tasks_run_triggers_execution() throws Exception {
        var taskId = UUID.randomUUID();
        var task = taskModel(taskId);
        when(taskQueryService.getById(taskId)).thenReturn(task);

        mockMvc.perform(post("/api/tasks/{id}/run", taskId)
                .param("role", "BACKEND_DEV"))
            .andExpect(status().isAccepted());

        verify(agentOrchestrator).run(taskId, AgentRole.BACKEND_DEV);
    }

    @Test
    void GET_tasks_id_returns_200_when_found() throws Exception {
        var id = UUID.randomUUID();
        when(taskQueryService.getById(id)).thenReturn(taskModel(id));

        mockMvc.perform(get("/api/tasks/{id}", id).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void GET_tasks_id_returns_404_when_missing() throws Exception {
        var id = UUID.randomUUID();
        when(taskQueryService.getById(id)).thenThrow(new NotFoundException("Task", id));

        mockMvc.perform(get("/api/tasks/{id}", id).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    void POST_tasks_returns_400_when_title_missing() throws Exception {
        var body = """
            {"description":"TDD","role":"BACKEND_DEV"}
            """;

        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    private TaskModel taskModel(UUID id) {
        return new TaskModel(
            id, null, "title", "description",
            AgentRole.BACKEND_DEV, TaskStatus.PENDING, null,
            Instant.now(), Instant.now()
        );
    }
}
