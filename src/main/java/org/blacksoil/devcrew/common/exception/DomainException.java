package org.blacksoil.devcrew.common.exception;

/**
 * Базовое исключение доменного слоя. Не содержит Spring-зависимостей.
 */
public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }

    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
