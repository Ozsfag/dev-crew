package org.blacksoil.devcrew.auth.adapter.out.persistence;

import org.blacksoil.devcrew.auth.domain.RefreshTokenModel;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RefreshTokenPersistenceMapper {

    RefreshTokenEntity toEntity(RefreshTokenModel model);

    RefreshTokenModel toModel(RefreshTokenEntity entity);
}
