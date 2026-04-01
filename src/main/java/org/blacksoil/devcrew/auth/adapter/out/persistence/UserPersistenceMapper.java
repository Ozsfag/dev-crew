package org.blacksoil.devcrew.auth.adapter.out.persistence;

import org.blacksoil.devcrew.auth.domain.UserModel;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserPersistenceMapper {

    UserEntity toEntity(UserModel model);

    UserModel toModel(UserEntity entity);
}
