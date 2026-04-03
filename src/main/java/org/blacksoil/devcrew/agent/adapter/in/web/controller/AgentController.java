package org.blacksoil.devcrew.agent.adapter.in.web.controller;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.blacksoil.devcrew.agent.adapter.in.web.dto.AgentResponse;
import org.blacksoil.devcrew.agent.adapter.in.web.mapper.AgentWebMapper;
import org.blacksoil.devcrew.agent.app.service.query.AgentQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
public class AgentController {

  private final AgentQueryService agentQueryService;
  private final AgentWebMapper mapper;

  @GetMapping
  public List<AgentResponse> getAll() {
    return agentQueryService.getAll().stream().map(mapper::toResponse).toList();
  }

  @GetMapping("/{id}")
  public ResponseEntity<AgentResponse> getById(@PathVariable UUID id) {
    return ResponseEntity.ok(mapper.toResponse(agentQueryService.getById(id)));
  }
}
