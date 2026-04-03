package org.blacksoil.devcrew.agent.adapter.in.web.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.blacksoil.devcrew.agent.adapter.in.web.mapper.AgentWebMapper;
import org.blacksoil.devcrew.agent.app.service.query.AgentQueryService;
import org.blacksoil.devcrew.agent.domain.AgentModel;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.agent.domain.AgentStatus;
import org.blacksoil.devcrew.common.exception.NotFoundException;
import org.blacksoil.devcrew.common.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AgentControllerTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  @Mock private AgentQueryService agentQueryService;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    var controller =
        new AgentController(agentQueryService, Mappers.getMapper(AgentWebMapper.class));
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  void GET_agents_returns_200_with_list() throws Exception {
    when(agentQueryService.getAll())
        .thenReturn(List.of(agentModel(AgentRole.BACKEND_DEV), agentModel(AgentRole.QA)));

    mockMvc
        .perform(get("/api/agents").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].role").value("BACKEND_DEV"));
  }

  @Test
  void GET_agents_id_returns_200_when_found() throws Exception {
    var id = UUID.randomUUID();
    when(agentQueryService.getById(id)).thenReturn(agentModel(id, AgentRole.QA));

    mockMvc
        .perform(get("/api/agents/{id}", id).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.role").value("QA"))
        .andExpect(jsonPath("$.status").value("IDLE"));
  }

  @Test
  void GET_agents_id_returns_404_when_missing() throws Exception {
    var id = UUID.randomUUID();
    when(agentQueryService.getById(id)).thenThrow(new NotFoundException("Agent", id));

    mockMvc
        .perform(get("/api/agents/{id}", id).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));
  }

  private AgentModel agentModel(AgentRole role) {
    return agentModel(UUID.randomUUID(), role);
  }

  private AgentModel agentModel(UUID id, AgentRole role) {
    return new AgentModel(id, role, AgentStatus.IDLE, "prompt", Instant.now(), Instant.now());
  }
}
