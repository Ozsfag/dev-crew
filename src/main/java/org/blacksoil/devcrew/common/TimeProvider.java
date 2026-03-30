package org.blacksoil.devcrew.common;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Абстракция над системным временем — упрощает тестирование.
 * Используй вместо Instant.now() / LocalDate.now().
 */
@Component
public class TimeProvider {

    public Instant now() {
        return Instant.now();
    }

    public LocalDate today() {
        return LocalDate.now();
    }
}
