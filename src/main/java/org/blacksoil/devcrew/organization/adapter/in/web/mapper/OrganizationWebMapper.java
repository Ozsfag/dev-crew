package org.blacksoil.devcrew.organization.adapter.in.web.mapper;

import org.blacksoil.devcrew.organization.adapter.in.web.dto.OrganizationResponse;
import org.blacksoil.devcrew.organization.adapter.in.web.dto.ProjectResponse;
import org.blacksoil.devcrew.organization.domain.OrganizationModel;
import org.blacksoil.devcrew.organization.domain.ProjectModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrganizationWebMapper {

  @Mapping(target = "createdAt", source = "createdAt")
  OrganizationResponse toResponse(OrganizationModel model);

  @Mapping(target = "createdAt", source = "createdAt")
  ProjectResponse toResponse(ProjectModel model);
}
