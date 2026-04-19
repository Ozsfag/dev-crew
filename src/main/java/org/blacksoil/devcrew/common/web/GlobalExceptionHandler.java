package org.blacksoil.devcrew.common.web;

import java.time.format.DateTimeParseException;
import org.blacksoil.devcrew.auth.domain.AuthException;
import org.blacksoil.devcrew.common.exception.ConflictException;
import org.blacksoil.devcrew.common.exception.DomainException;
import org.blacksoil.devcrew.common.exception.ForbiddenException;
import org.blacksoil.devcrew.common.exception.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(AuthException.class)
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public ErrorResponse handleAuth(AuthException ex) {
    return new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), ex.getMessage());
  }

  @ExceptionHandler(ForbiddenException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public ErrorResponse handleForbidden(ForbiddenException ex) {
    return new ErrorResponse(HttpStatus.FORBIDDEN.value(), ex.getMessage());
  }

  @ExceptionHandler(ConflictException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public ErrorResponse handleConflict(ConflictException ex) {
    return new ErrorResponse(HttpStatus.CONFLICT.value(), ex.getMessage());
  }

  @ExceptionHandler(NotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ErrorResponse handleNotFound(NotFoundException ex) {
    return new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorResponse handleValidation(MethodArgumentNotValidException ex) {
    var message =
        ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .findFirst()
            .orElse("Ошибка валидации");
    return new ErrorResponse(HttpStatus.BAD_REQUEST.value(), message);
  }

  @ExceptionHandler(DomainException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorResponse handleDomain(DomainException ex) {
    return new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
  }

  @ExceptionHandler(DateTimeParseException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorResponse handleDateTimeParse(DateTimeParseException ex) {
    return new ErrorResponse(
        HttpStatus.BAD_REQUEST.value(), "Неверный формат даты: " + ex.getParsedString());
  }

  @ExceptionHandler(UnsupportedOperationException.class)
  @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
  public ErrorResponse handleUnsupported(UnsupportedOperationException ex) {
    return new ErrorResponse(HttpStatus.NOT_IMPLEMENTED.value(), ex.getMessage());
  }
}
