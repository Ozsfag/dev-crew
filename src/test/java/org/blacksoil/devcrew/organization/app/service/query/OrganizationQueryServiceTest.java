package org.blacksoil.devcrew.organization.app.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.blacksoil.devcrew.common.exception.DomainException;
import org.blacksoil.devcrew.organization.domain.OrgPlan;
import org.blacksoil.devcrew.organization.domain.model.OrganizationModel;
import org.blacksoil.devcrew.organization.domain.model.ProjectModel;
import org.blacksoil.devcrew.organization.domain.store.OrganizationStore;
import org.blacksoil.devcrew.organization.domain.store.ProjectStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizationQueryServiceTest {

  @Mock private OrganizationStore organizationStore;
  @Mock private ProjectStore projectStore;

  @InjectMocks private OrganizationQueryService service;

  private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

  @Test
  void getById_returns_org_when_found() {
    var id = UUID.randomUUID();
    var org = org(id);
    when(organizationStore.findById(id)).thenReturn(Optional.of(org));

    var result = service.getById(id);

    assertThat(result).isEqualTo(org);
  }

  @Test
  void getById_throws_domain_exception_when_not_found() {
    var id = UUID.randomUUID();
    when(organizationStore.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getById(id))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining(id.toString());
  }

  @Test
  void getProjectsByOrg_returns_projects_for_org() {
    var orgId = UUID.randomUUID();
    var projects = List.of(project(orgId), project(orgId));
    when(projectStore.findByOrgId(orgId)).thenReturn(projects);

    var result = service.getProjectsByOrg(orgId);

    assertThat(result).isEqualTo(projects);
  }

  @Test
  void getProjectsByOrg_returns_empty_list_when_no_projects() {
    var orgId = UUID.randomUUID();
    when(projectStore.findByOrgId(orgId)).thenReturn(List.of());

    var result = service.getProjectsByOrg(orgId);

    assertThat(result).isEmpty();
  }

  @Test
  void getProjectById_returns_project_when_found() {
    var projectId = UUID.randomUUID();
    var proj = project(UUID.randomUUID());
    when(projectStore.findById(projectId)).thenReturn(Optional.of(proj));

    var result = service.getProjectById(projectId);

    assertThat(result).isEqualTo(proj);
  }

  @Test
  void getProjectById_throws_domain_exception_when_not_found() {
    var projectId = UUID.randomUUID();
    when(projectStore.findById(projectId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getProjectById(projectId))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining(projectId.toString());
  }

  private OrganizationModel org(UUID id) {
    return new OrganizationModel(id, "Acme Corp", OrgPlan.FREE, null, NOW, NOW);
  }

  private ProjectModel project(UUID orgId) {
    return new ProjectModel(UUID.randomUUID(), orgId, "Backend", "/repos/backend", NOW, NOW);
  }
}
