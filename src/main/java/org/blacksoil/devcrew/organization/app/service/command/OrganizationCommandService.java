package org.blacksoil.devcrew.organization.app.service.command;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.blacksoil.devcrew.common.TimeProvider;
import org.blacksoil.devcrew.organization.domain.OrgPlan;
import org.blacksoil.devcrew.organization.domain.model.OrganizationModel;
import org.blacksoil.devcrew.organization.domain.model.ProjectModel;
import org.blacksoil.devcrew.organization.domain.store.OrganizationStore;
import org.blacksoil.devcrew.organization.domain.store.ProjectStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrganizationCommandService {

  private final OrganizationStore organizationStore;
  private final ProjectStore projectStore;
  private final TimeProvider timeProvider;

  @Transactional
  public OrganizationModel createOrganization(String name) {
    var now = timeProvider.now();
    var org = new OrganizationModel(UUID.randomUUID(), name, OrgPlan.FREE, now, now);
    return organizationStore.save(org);
  }

  @Transactional
  public ProjectModel createProject(UUID orgId, String name, String repoPath) {
    var now = timeProvider.now();
    var project = new ProjectModel(UUID.randomUUID(), orgId, name, repoPath, now, now);
    return projectStore.save(project);
  }
}
