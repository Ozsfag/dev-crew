package org.blacksoil.devcrew.auth.adapter.out.persistence.mapper;

import org.blacksoil.devcrew.auth.adapter.out.persistence.entity.UserEntity;
import org.blacksoil.devcrew.auth.domain.model.UserModel;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserPersistenceMapper {

  UserEntity toEntity(UserModel model);

  UserModel toModel(UserEntity entity);
}
