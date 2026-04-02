package org.blacksoil.devcrew.organization.adapter.out.persistence;

import org.blacksoil.devcrew.organization.domain.ProjectModel;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProjectPersistenceMapper {
    ProjectEntity toEntity(ProjectModel model);
    ProjectModel toModel(ProjectEntity entity);
}
