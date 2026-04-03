package org.blacksoil.devcrew.agent.app.service.query;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.blacksoil.devcrew.agent.domain.AgentModel;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.agent.domain.AgentStore;
import org.blacksoil.devcrew.common.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AgentQueryService {

  private final AgentStore agentStore;

  public AgentModel getById(UUID id) {
    return agentStore.findById(id).orElseThrow(() -> new NotFoundException("Agent", id));
  }

  public AgentModel getByRole(AgentRole role) {
    return agentStore
        .findByRole(role)
        .orElseThrow(() -> new NotFoundException("Agent[role=" + role + "]", null));
  }

  public List<AgentModel> getAll() {
    return agentStore.findAll();
  }
}
