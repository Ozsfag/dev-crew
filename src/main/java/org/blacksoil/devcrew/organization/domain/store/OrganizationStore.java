package org.blacksoil.devcrew.organization.domain.store;

import java.util.Optional;
import java.util.UUID;
import org.blacksoil.devcrew.organization.domain.model.OrganizationModel;

public interface OrganizationStore {
  OrganizationModel save(OrganizationModel organization);

  Optional<OrganizationModel> findById(UUID id);
}
