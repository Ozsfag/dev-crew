package org.blacksoil.devcrew.auth.adapter.out.persistence.mapper;

import org.blacksoil.devcrew.auth.adapter.out.persistence.entity.RefreshTokenEntity;
import org.blacksoil.devcrew.auth.domain.model.RefreshTokenModel;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RefreshTokenPersistenceMapper {

  RefreshTokenEntity toEntity(RefreshTokenModel model);

  RefreshTokenModel toModel(RefreshTokenEntity entity);
}
