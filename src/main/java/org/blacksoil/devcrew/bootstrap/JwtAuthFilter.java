package org.blacksoil.devcrew.bootstrap;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SecurityException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.blacksoil.devcrew.auth.app.service.JwtService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtService jwtService;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    var authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
      filterChain.doFilter(request, response);
      return;
    }

    var token = authHeader.substring(BEARER_PREFIX.length());
    try {
      jwtService.validateAccessToken(token);
      var userId = jwtService.extractUserId(token);
      var orgId = jwtService.extractOrgId(token);
      var role = jwtService.extractRole(token);
      var principal = new AuthenticatedUser(userId, orgId, role);
      var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
      var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
      SecurityContextHolder.getContext().setAuthentication(authentication);
    } catch (ExpiredJwtException e) {
      log.debug("JWT токен просрочен: {}", e.getMessage());
    } catch (MalformedJwtException | SecurityException e) {
      log.warn("Невалидный JWT токен: {}", e.getMessage());
    } catch (Exception e) {
      log.error("Непредвиденная ошибка при валидации JWT", e);
    }

    filterChain.doFilter(request, response);
  }
}
