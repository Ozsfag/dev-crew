package org.blacksoil.devcrew.agent.adapter.out.persistence.mapper;

import org.blacksoil.devcrew.agent.adapter.out.persistence.entity.AgentEntity;
import org.blacksoil.devcrew.agent.domain.model.AgentModel;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AgentPersistenceMapper {

  AgentModel toModel(AgentEntity entity);

  AgentEntity toEntity(AgentModel model);
}
