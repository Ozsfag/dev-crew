package org.blacksoil.devcrew.organization.domain;

import java.util.Optional;
import java.util.UUID;

public interface OrganizationStore {
  OrganizationModel save(OrganizationModel organization);

  Optional<OrganizationModel> findById(UUID id);
}
