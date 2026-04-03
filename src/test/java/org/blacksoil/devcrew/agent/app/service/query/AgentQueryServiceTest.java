package org.blacksoil.devcrew.agent.app.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.agent.domain.AgentStatus;
import org.blacksoil.devcrew.agent.domain.model.AgentModel;
import org.blacksoil.devcrew.agent.domain.store.AgentStore;
import org.blacksoil.devcrew.common.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AgentQueryServiceTest {

  @Mock private AgentStore agentStore;

  @InjectMocks private AgentQueryService agentQueryService;

  @Test
  void getById_returns_agent_when_found() {
    var id = UUID.randomUUID();
    var agent = agentModel(id, AgentRole.BACKEND_DEV);
    when(agentStore.findById(id)).thenReturn(Optional.of(agent));

    var result = agentQueryService.getById(id);

    assertThat(result).isEqualTo(agent);
  }

  @Test
  void getById_throws_not_found_when_missing() {
    var id = UUID.randomUUID();
    when(agentStore.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> agentQueryService.getById(id)).isInstanceOf(NotFoundException.class);
  }

  @Test
  void getByRole_returns_agent_for_role() {
    var agent = agentModel(UUID.randomUUID(), AgentRole.QA);
    when(agentStore.findByRole(AgentRole.QA)).thenReturn(Optional.of(agent));

    var result = agentQueryService.getByRole(AgentRole.QA);

    assertThat(result.role()).isEqualTo(AgentRole.QA);
  }

  @Test
  void getAll_returns_all_agents() {
    var agents =
        List.of(
            agentModel(UUID.randomUUID(), AgentRole.BACKEND_DEV),
            agentModel(UUID.randomUUID(), AgentRole.QA));
    when(agentStore.findAll()).thenReturn(agents);

    var result = agentQueryService.getAll();

    assertThat(result).hasSize(2);
  }

  private AgentModel agentModel(UUID id, AgentRole role) {
    return new AgentModel(
        id, role, AgentStatus.IDLE, "system prompt", Instant.now(), Instant.now());
  }
}
