package org.blacksoil.devcrew.organization.adapter.out.persistence;

import org.blacksoil.devcrew.organization.domain.OrganizationModel;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrganizationPersistenceMapper {
    OrganizationEntity toEntity(OrganizationModel model);
    OrganizationModel toModel(OrganizationEntity entity);
}
