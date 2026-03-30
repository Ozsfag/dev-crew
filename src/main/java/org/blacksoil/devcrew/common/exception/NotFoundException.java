package org.blacksoil.devcrew.common.exception;

import java.util.UUID;

public class NotFoundException extends DomainException {

    public NotFoundException(String entity, UUID id) {
        super(entity + " не найден: " + id);
    }
}
