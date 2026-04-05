package org.blacksoil.devcrew.auth.domain;

import java.util.UUID;

/** Port для создания организации при регистрации нового пользователя. */
public interface OrganizationCreationPort {

  UUID createForUser(String name);
}
