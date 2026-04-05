package org.blacksoil.devcrew.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SecurityException;
import java.util.UUID;
import org.blacksoil.devcrew.auth.app.service.JwtService;
import org.blacksoil.devcrew.auth.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

  @Mock private JwtService jwtService;

  private JwtAuthFilter filter;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
    filter = new JwtAuthFilter(jwtService);
  }

  @Test
  void doFilterInternal_valid_token_sets_authentication() throws Exception {
    var userId = UUID.randomUUID();
    var orgId = UUID.randomUUID();
    var request = requestWithToken("valid.jwt.token");
    doNothing().when(jwtService).validateAccessToken("valid.jwt.token");
    when(jwtService.extractUserId("valid.jwt.token")).thenReturn(userId);
    when(jwtService.extractOrgId("valid.jwt.token")).thenReturn(orgId);
    when(jwtService.extractRole("valid.jwt.token")).thenReturn(UserRole.ARCHITECT);

    filter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

    var auth = SecurityContextHolder.getContext().getAuthentication();
    assertThat(auth).isNotNull();
    assertThat(auth.getPrincipal()).isInstanceOf(AuthenticatedUser.class);
    var principal = (AuthenticatedUser) auth.getPrincipal();
    assertThat(principal.userId()).isEqualTo(userId);
    assertThat(principal.orgId()).isEqualTo(orgId);
  }

  @Test
  void doFilterInternal_no_header_passes_through_without_auth() throws Exception {
    var request = new MockHttpServletRequest();
    var chain = new MockFilterChain();

    filter.doFilterInternal(request, new MockHttpServletResponse(), chain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    assertThat(chain.getRequest()).isNotNull();
  }

  @Test
  void doFilterInternal_expired_token_clears_auth_and_continues() throws Exception {
    var request = requestWithToken("expired.token");
    doThrow(mock(ExpiredJwtException.class)).when(jwtService).validateAccessToken("expired.token");
    var chain = new MockFilterChain();

    filter.doFilterInternal(request, new MockHttpServletResponse(), chain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    assertThat(chain.getRequest()).isNotNull();
  }

  @Test
  void doFilterInternal_malformed_token_clears_auth_and_continues() throws Exception {
    var request = requestWithToken("bad.token");
    doThrow(mock(MalformedJwtException.class)).when(jwtService).validateAccessToken("bad.token");
    var chain = new MockFilterChain();

    filter.doFilterInternal(request, new MockHttpServletResponse(), chain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    assertThat(chain.getRequest()).isNotNull();
  }

  @Test
  void doFilterInternal_security_exception_clears_auth_and_continues() throws Exception {
    var request = requestWithToken("tampered.token");
    doThrow(mock(SecurityException.class)).when(jwtService).validateAccessToken("tampered.token");
    var chain = new MockFilterChain();

    filter.doFilterInternal(request, new MockHttpServletResponse(), chain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    assertThat(chain.getRequest()).isNotNull();
  }

  @Test
  void doFilterInternal_unexpected_exception_clears_auth_and_continues() throws Exception {
    var request = requestWithToken("broken.token");
    doThrow(new IllegalStateException("unexpected"))
        .when(jwtService)
        .validateAccessToken("broken.token");
    var chain = new MockFilterChain();

    filter.doFilterInternal(request, new MockHttpServletResponse(), chain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    assertThat(chain.getRequest()).isNotNull();
  }

  private MockHttpServletRequest requestWithToken(String token) {
    var request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer " + token);
    return request;
  }
}
