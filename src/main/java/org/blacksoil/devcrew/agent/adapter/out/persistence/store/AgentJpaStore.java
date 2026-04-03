package org.blacksoil.devcrew.agent.adapter.out.persistence.store;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.blacksoil.devcrew.agent.adapter.out.persistence.mapper.AgentPersistenceMapper;
import org.blacksoil.devcrew.agent.adapter.out.persistence.repository.AgentRepository;
import org.blacksoil.devcrew.agent.domain.AgentModel;
import org.blacksoil.devcrew.agent.domain.AgentRole;
import org.blacksoil.devcrew.agent.domain.AgentStore;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class AgentJpaStore implements AgentStore {

  private final AgentRepository agentRepository;
  private final AgentPersistenceMapper mapper;

  @Override
  @Transactional
  public AgentModel save(AgentModel agent) {
    var entity = mapper.toEntity(agent);
    return mapper.toModel(agentRepository.save(entity));
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<AgentModel> findById(UUID id) {
    return agentRepository.findById(id).map(mapper::toModel);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<AgentModel> findByRole(AgentRole role) {
    return agentRepository.findByRole(role).map(mapper::toModel);
  }

  @Override
  @Transactional(readOnly = true)
  public List<AgentModel> findAll() {
    return agentRepository.findAll().stream().map(mapper::toModel).toList();
  }
}
