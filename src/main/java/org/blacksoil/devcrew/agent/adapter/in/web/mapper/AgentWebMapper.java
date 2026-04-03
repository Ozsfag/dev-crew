package org.blacksoil.devcrew.agent.adapter.in.web.mapper;

import org.blacksoil.devcrew.agent.adapter.in.web.dto.AgentResponse;
import org.blacksoil.devcrew.agent.domain.model.AgentModel;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AgentWebMapper {

  AgentResponse toResponse(AgentModel model);
}
