package org.blacksoil.devcrew.common.web;

/** Унифицированный формат ошибки в HTTP-ответах. */
public record ErrorResponse(int status, String message) {}
