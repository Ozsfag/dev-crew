package org.blacksoil.devcrew.organization.adapter.in.hook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.blacksoil.devcrew.organization.app.service.command.OrganizationCommandService;
import org.blacksoil.devcrew.organization.domain.OrgPlan;
import org.blacksoil.devcrew.organization.domain.model.OrganizationModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizationCreationAdapterTest {

  private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

  @Mock private OrganizationCommandService commandService;

  @InjectMocks private OrganizationCreationAdapter adapter;

  @Test
  void createForUser_delegates_to_command_service_and_returns_org_id() {
    var orgId = UUID.randomUUID();
    var org = new OrganizationModel(orgId, "Acme Corp", OrgPlan.FREE, null, NOW, NOW);
    when(commandService.createOrganization("Acme Corp")).thenReturn(org);

    var result = adapter.createForUser("Acme Corp");

    assertThat(result).isEqualTo(orgId);
  }
}
