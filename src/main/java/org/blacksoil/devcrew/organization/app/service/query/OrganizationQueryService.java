package org.blacksoil.devcrew.organization.app.service.query;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.blacksoil.devcrew.common.exception.DomainException;
import org.blacksoil.devcrew.organization.domain.OrganizationModel;
import org.blacksoil.devcrew.organization.domain.OrganizationStore;
import org.blacksoil.devcrew.organization.domain.ProjectModel;
import org.blacksoil.devcrew.organization.domain.ProjectStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrganizationQueryService {

  private final OrganizationStore organizationStore;
  private final ProjectStore projectStore;

  @Transactional(readOnly = true)
  public OrganizationModel getById(UUID id) {
    return organizationStore
        .findById(id)
        .orElseThrow(() -> new DomainException("Organization not found: " + id));
  }

  @Transactional(readOnly = true)
  public List<ProjectModel> getProjectsByOrg(UUID orgId) {
    return projectStore.findByOrgId(orgId);
  }

  @Transactional(readOnly = true)
  public ProjectModel getProjectById(UUID projectId) {
    return projectStore
        .findById(projectId)
        .orElseThrow(() -> new DomainException("Project not found: " + projectId));
  }
}
