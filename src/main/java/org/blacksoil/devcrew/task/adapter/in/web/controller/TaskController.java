package org.blacksoil.devcrew.task.adapter.in.web.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.blacksoil.devcrew.agent.domain.AgentOrchestrator;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.bootstrap.AuthenticatedUser;
import org.blacksoil.devcrew.task.adapter.in.web.dto.CreateTaskRequest;
import org.blacksoil.devcrew.task.adapter.in.web.dto.CreateTaskResponse;
import org.blacksoil.devcrew.task.adapter.in.web.dto.TaskResponse;
import org.blacksoil.devcrew.task.adapter.in.web.mapper.TaskWebMapper;
import org.blacksoil.devcrew.task.app.service.query.TaskQueryService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

  private final TaskQueryService taskQueryService;
  private final AgentOrchestrator agentOrchestrator;
  private final TaskWebMapper mapper;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CreateTaskResponse create(@Valid @RequestBody CreateTaskRequest request) {
    var taskId =
        agentOrchestrator.submit(
            request.title(), request.description(), request.role(), request.projectId());
    return new CreateTaskResponse(taskId);
  }

  @GetMapping
  public List<TaskResponse> getByProject(
      @RequestParam UUID projectId, @AuthenticationPrincipal AuthenticatedUser currentUser) {
    return taskQueryService.getByProjectId(projectId).stream().map(mapper::toResponse).toList();
  }

  @PostMapping("/{id}/run")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public void run(@PathVariable UUID id, @RequestParam AgentRole role) {
    taskQueryService.getById(id);
    agentOrchestrator.run(id, role);
  }

  @GetMapping("/{id}")
  public TaskResponse getById(@PathVariable UUID id) {
    return mapper.toResponse(taskQueryService.getById(id));
  }
}
