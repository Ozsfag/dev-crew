package org.blacksoil.devcrew.task.adapter.in.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.blacksoil.devcrew.agent.domain.AgentOrchestrator;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.task.adapter.in.web.dto.CreateTaskRequest;
import org.blacksoil.devcrew.task.adapter.in.web.dto.CreateTaskResponse;
import org.blacksoil.devcrew.task.adapter.in.web.dto.TaskResponse;
import org.blacksoil.devcrew.task.app.service.query.TaskQueryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

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
        var taskId = agentOrchestrator.submit(request.title(), request.description(), request.role());
        return new CreateTaskResponse(taskId);
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
