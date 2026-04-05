package org.blacksoil.devcrew.organization.adapter.in.hook;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.blacksoil.devcrew.auth.domain.OrganizationCreationPort;
import org.blacksoil.devcrew.organization.app.service.command.OrganizationCommandService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrganizationCreationAdapter implements OrganizationCreationPort {

  private final OrganizationCommandService commandService;

  @Override
  public UUID createForUser(String name) {
    return commandService.createOrganization(name).id();
  }
}
