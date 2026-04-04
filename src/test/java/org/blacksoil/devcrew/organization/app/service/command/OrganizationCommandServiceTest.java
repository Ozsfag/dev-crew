package org.blacksoil.devcrew.organization.app.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.blacksoil.devcrew.common.TimeProvider;
import org.blacksoil.devcrew.common.exception.DomainException;
import org.blacksoil.devcrew.organization.domain.OrgPlan;
import org.blacksoil.devcrew.organization.domain.model.OrganizationModel;
import org.blacksoil.devcrew.organization.domain.model.ProjectModel;
import org.blacksoil.devcrew.organization.domain.store.OrganizationStore;
import org.blacksoil.devcrew.organization.domain.store.ProjectStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizationCommandServiceTest {

  @Mock private OrganizationStore organizationStore;
  @Mock private ProjectStore projectStore;
  @Mock private TimeProvider timeProvider;

  @InjectMocks private OrganizationCommandService service;

  private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

  @Test
  void createOrganization_saves_org_with_free_plan() {
    when(timeProvider.now()).thenReturn(NOW);
    when(organizationStore.save(any())).thenAnswer(inv -> inv.getArgument(0));

    service.createOrganization("Acme Corp");

    var captor = ArgumentCaptor.<OrganizationModel>captor();
    verify(organizationStore).save(captor.capture());
    var saved = captor.getValue();
    assertThat(saved.name()).isEqualTo("Acme Corp");
    assertThat(saved.plan()).isEqualTo(OrgPlan.FREE);
    assertThat(saved.createdAt()).isEqualTo(NOW);
    assertThat(saved.updatedAt()).isEqualTo(NOW);
    assertThat(saved.id()).isNotNull();
  }

  @Test
  void createOrganization_returns_saved_model() {
    when(timeProvider.now()).thenReturn(NOW);
    var expected = org("Acme Corp");
    when(organizationStore.save(any())).thenReturn(expected);

    var result = service.createOrganization("Acme Corp");

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void createProject_saves_project_with_orgId() {
    var orgId = UUID.randomUUID();
    when(timeProvider.now()).thenReturn(NOW);
    when(projectStore.save(any())).thenAnswer(inv -> inv.getArgument(0));

    service.createProject(orgId, "Backend", "/repos/backend");

    var captor = ArgumentCaptor.<ProjectModel>captor();
    verify(projectStore).save(captor.capture());
    var saved = captor.getValue();
    assertThat(saved.orgId()).isEqualTo(orgId);
    assertThat(saved.name()).isEqualTo("Backend");
    assertThat(saved.repoPath()).isEqualTo("/repos/backend");
    assertThat(saved.createdAt()).isEqualTo(NOW);
    assertThat(saved.id()).isNotNull();
  }

  @Test
  void createProject_returns_saved_model() {
    var orgId = UUID.randomUUID();
    when(timeProvider.now()).thenReturn(NOW);
    var expected = project(orgId);
    when(projectStore.save(any())).thenReturn(expected);

    var result = service.createProject(orgId, "Backend", "/repos/backend");

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void updatePlanByStripeCustomer_upgrades_plan_and_saves() {
    var customerId = "cus_test123";
    var existing = org("Acme Corp");
    when(organizationStore.findByStripeCustomerId(customerId)).thenReturn(Optional.of(existing));
    when(timeProvider.now()).thenReturn(NOW);
    when(organizationStore.save(any())).thenAnswer(inv -> inv.getArgument(0));

    var result = service.updatePlanByStripeCustomer(customerId, OrgPlan.PRO);

    assertThat(result.plan()).isEqualTo(OrgPlan.PRO);
    assertThat(result.updatedAt()).isEqualTo(NOW);
    verify(organizationStore).save(any());
  }

  @Test
  void updatePlanByStripeCustomer_throws_when_customer_not_found() {
    var customerId = "cus_unknown";
    when(organizationStore.findByStripeCustomerId(customerId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.updatePlanByStripeCustomer(customerId, OrgPlan.PRO))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining(customerId);
  }

  private OrganizationModel org(String name) {
    return new OrganizationModel(UUID.randomUUID(), name, OrgPlan.FREE, null, NOW, NOW);
  }

  private ProjectModel project(UUID orgId) {
    return new ProjectModel(UUID.randomUUID(), orgId, "Backend", "/repos/backend", NOW, NOW);
  }
}
