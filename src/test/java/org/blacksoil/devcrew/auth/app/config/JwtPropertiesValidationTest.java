package org.blacksoil.devcrew.auth.app.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JwtPropertiesValidationTest {

  @Test
  void secret_fails_validation_when_blank() {
    var validator = Validation.buildDefaultValidatorFactory().getValidator();
    var properties = new JwtProperties();
    properties.setSecret("");

    var violations = validator.validate(properties);

    assertThat(violations).isNotEmpty();
    assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("secret"));
  }

  @Test
  void secret_fails_validation_when_shorter_than_32_chars() {
    var validator = Validation.buildDefaultValidatorFactory().getValidator();
    var properties = new JwtProperties();
    properties.setSecret("short");

    var violations = validator.validate(properties);

    assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("secret"));
  }

  @Test
  void secret_passes_validation_when_32_chars_or_longer() {
    var validator = Validation.buildDefaultValidatorFactory().getValidator();
    var properties = new JwtProperties();
    properties.setSecret("a".repeat(32));

    var violations = validator.validate(properties);

    assertThat(violations).isEmpty();
  }
}
