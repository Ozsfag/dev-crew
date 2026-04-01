package org.blacksoil.devcrew.agent.adapter.out.persistence;

import org.blacksoil.devcrew.agent.domain.AgentModel;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AgentPersistenceMapper {

    AgentModel toModel(AgentEntity entity);

    AgentEntity toEntity(AgentModel model);
}
