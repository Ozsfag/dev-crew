package org.blacksoil.devcrew.billing.adapter.in.stripe;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.blacksoil.devcrew.billing.app.config.BillingProperties;
import org.blacksoil.devcrew.billing.domain.StripeWebhookPort;
import org.blacksoil.devcrew.common.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class StripeWebhookAdapterTest {

  @Mock private StripeWebhookPort stripeWebhookPort;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    var properties = new BillingProperties();
    properties.setStripeWebhookSecret("whsec_test_secret");
    var adapter = new StripeWebhookAdapter(stripeWebhookPort, properties);
    mockMvc =
        MockMvcBuilders.standaloneSetup(adapter)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  void POST_webhook_returns_400_for_invalid_signature() throws Exception {
    mockMvc
        .perform(
            post("/api/stripe/webhook")
                .contentType(MediaType.TEXT_PLAIN)
                .header("Stripe-Signature", "invalid_signature")
                .content("{}"))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(stripeWebhookPort);
  }

  @Test
  void POST_webhook_returns_400_when_signature_missing() throws Exception {
    mockMvc
        .perform(post("/api/stripe/webhook").contentType(MediaType.TEXT_PLAIN).content("{}"))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(stripeWebhookPort);
  }
}
