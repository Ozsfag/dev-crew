package org.blacksoil.devcrew.auth.domain;

import org.blacksoil.devcrew.common.exception.DomainException;

public class AuthException extends DomainException {

    public AuthException(String message) {
        super(message);
    }
}
