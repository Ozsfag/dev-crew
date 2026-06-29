package org.blacksoil.devcrew.notification.adapter.out.persistence.mapper;

import org.blacksoil.devcrew.notification.adapter.out.persistence.entity.ChatSessionEntity;
import org.blacksoil.devcrew.notification.domain.model.ChatSessionModel;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ChatSessionPersistenceMapper {

  ChatSessionModel toModel(ChatSessionEntity entity);

  ChatSessionEntity toEntity(ChatSessionModel model);
}
