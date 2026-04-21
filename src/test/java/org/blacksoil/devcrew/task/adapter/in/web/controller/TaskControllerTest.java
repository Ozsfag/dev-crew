package org.blacksoil.devcrew.task.adapter.in.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.blacksoil.devcrew.agent.domain.AgentOrchestrator;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.auth.domain.UserRole;
import org.blacksoil.devcrew.bootstrap.AuthenticatedUser;
import org.blacksoil.devcrew.common.PageResult;
import org.blacksoil.devcrew.common.exception.NotFoundException;
import org.blacksoil.devcrew.common.web.GlobalExceptionHandler;
import org.blacksoil.devcrew.task.adapter.in.web.mapper.TaskWebMapper;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class TaskControllerTest {

  private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
  private static final UUID ORG_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID OTHER_ORG_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
  private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

  private final ObjectMapper objectMapper = new ObjectMapper();
  @Mock private TaskQueryService taskQueryService;
  @Mock private AgentOrchestrator agentOrchestrator;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    var controller =
        new TaskController(
            taskQueryService, agentOrchestrator, Mappers.getMapper(TaskWebMapper.class));
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build();
  }

  @Test
  void POST_tasks_returns_201_with_task_id() throws Exception {
    var taskId = UUID.randomUUID();
    when(agentOrchestrator.submit(any(), any(), any(), any(), any())).thenReturn(taskId);

    var body =
        """
            {"title":"Write tests","description":"TDD for UserService","role":"BACKEND_DEV"}
            """;

    mockMvc
        .perform(
            post("/api/tasks")
                .with(principalAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.taskId").value(taskId.toString()));
  }

  @Test
  void POST_tasks_delegates_to_orchestrator_with_correct_params() throws Exception {
    when(agentOrchestrator.submit(any(), any(), any(), any(), any())).thenReturn(UUID.randomUUID());

    var body =
        """
            {"title":"Write tests","description":"TDD","role":"QA"}
            """;

    mockMvc
        .perform(
            post("/api/tasks")
                .with(principalAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isCreated());

    verify(agentOrchestrator).submit("Write tests", "TDD", AgentRole.QA, null, ORG_ID);
  }

  @Test
  void GET_tasks_returns_page_filtered_by_org() throws Exception {
    var taskId = UUID.randomUUID();
    var pageResult = new PageResult<>(List.of(taskModel(taskId, ORG_ID)), 0, 20, 1);
    when(taskQueryService.getByOrgId(ORG_ID, 0, 20)).thenReturn(pageResult);

    mockMvc
        .perform(get("/api/tasks").with(principalAuth()).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(taskId.toString()))
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  void GET_tasks_passes_pagination_params() throws Exception {
    var pageResult = new PageResult<>(List.<TaskModel>of(), 1, 5, 0);
    when(taskQueryService.getByOrgId(ORG_ID, 1, 5)).thenReturn(pageResult);

    mockMvc
        .perform(
            get("/api/tasks")
                .param("page", "1")
                .param("size", "5")
                .with(principalAuth())
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.page").value(1))
        .andExpect(jsonPath("$.size").value(5));
  }

  @Test
  void POST_tasks_run_triggers_execution() throws Exception {
    var taskId = UUID.randomUUID();
    when(taskQueryService.getById(taskId)).thenReturn(taskModel(taskId, ORG_ID));

    mockMvc
        .perform(
            post("/api/tasks/{id}/run", taskId).with(principalAuth()).param("role", "BACKEND_DEV"))
        .andExpect(status().isAccepted());

    verify(agentOrchestrator).run(taskId, AgentRole.BACKEND_DEV);
  }

  @Test
  void POST_tasks_run_returns_403_when_wrong_org() throws Exception {
    var taskId = UUID.randomUUID();
    when(taskQueryService.getById(taskId)).thenReturn(taskModel(taskId, OTHER_ORG_ID));

    mockMvc
        .perform(
            post("/api/tasks/{id}/run", taskId).with(principalAuth()).param("role", "BACKEND_DEV"))
        .andExpect(status().isForbidden());
  }

  @Test
  void GET_tasks_id_returns_200_when_found() throws Exception {
    var id = UUID.randomUUID();
    when(taskQueryService.getById(id)).thenReturn(taskModel(id, ORG_ID));

    mockMvc
        .perform(
            get("/api/tasks/{id}", id).with(principalAuth()).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.status").value("PENDING"));
  }

  @Test
  void GET_tasks_id_returns_403_when_wrong_org() throws Exception {
    var id = UUID.randomUUID();
    when(taskQueryService.getById(id)).thenReturn(taskModel(id, OTHER_ORG_ID));

    mockMvc
        .perform(
            get("/api/tasks/{id}", id).with(principalAuth()).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());
  }

  @Test
  void GET_tasks_id_returns_404_when_missing() throws Exception {
    var id = UUID.randomUUID();
    when(taskQueryService.getById(id)).thenThrow(new NotFoundException("Task", id));

    mockMvc
        .perform(
            get("/api/tasks/{id}", id).with(principalAuth()).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  @Test
  void POST_tasks_returns_400_when_title_missing() throws Exception {
    var body =
        """
            {"description":"TDD","role":"BACKEND_DEV"}
            """;

    mockMvc
        .perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  void POST_tasks_returns_400_when_title_too_long() throws Exception {
    var body =
        "{\"title\":\"" + "x".repeat(501) + "\",\"description\":\"desc\",\"role\":\"BACKEND_DEV\"}";

    mockMvc
        .perform(
            post("/api/tasks")
                .with(principalAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  void POST_tasks_returns_400_when_description_too_long() throws Exception {
    var body =
        "{\"title\":\"title\",\"description\":\""
            + "x".repeat(20001)
            + "\",\"role\":\"BACKEND_DEV\"}";

    mockMvc
        .perform(
            post("/api/tasks")
                .with(principalAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest());
  }

  private static org.springframework.test.web.servlet.request.RequestPostProcessor principalAuth() {
    return request -> {
      var principal = new AuthenticatedUser(USER_ID, ORG_ID, UserRole.ARCHITECT);
      var auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());
      var ctx = SecurityContextHolder.createEmptyContext();
      ctx.setAuthentication(auth);
      SecurityContextHolder.setContext(ctx);
      return request;
    };
  }

  private TaskModel taskModel(UUID id, UUID orgId) {
    return new TaskModel(
        id,
        null,
        orgId,
        null,
        "title",
        "description",
        AgentRole.BACKEND_DEV,
        TaskStatus.PENDING,
        null,
        null,
        NOW,
        NOW,
        null);
  }
}
