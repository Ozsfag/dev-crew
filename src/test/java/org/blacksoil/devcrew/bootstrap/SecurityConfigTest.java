package org.blacksoil.devcrew.bootstrap;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.blacksoil.devcrew.common.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class SecurityConfigTest extends IntegrationTestBase {

  @Autowired private MockMvc mockMvc;

  @Test
  void GET_actuator_prometheus_returns_401_without_token() throws Exception {
    mockMvc.perform(get("/actuator/prometheus")).andExpect(status().isUnauthorized());
  }

  @Test
  void GET_actuator_health_returns_200_without_token() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }

  @Test
  void GET_api_returns_csp_header() throws Exception {
    // /api/auth/login — открытый endpoint, доступен без токена
    mockMvc
        .perform(
            get("/api/auth/login").contentType(org.springframework.http.MediaType.APPLICATION_JSON))
        .andExpect(
            header().string("Content-Security-Policy", containsString("default-src 'none'")));
  }
}
