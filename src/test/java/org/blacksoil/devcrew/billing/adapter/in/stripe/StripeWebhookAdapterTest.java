package org.blacksoil.devcrew.billing.adapter.in.stripe;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
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

  private static final String WEBHOOK_SECRET = "whsec_test_secret";

  @Mock private StripeWebhookPort stripeWebhookPort;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    var properties = new BillingProperties();
    properties.setStripeWebhookSecret(WEBHOOK_SECRET);
    var adapter = new StripeWebhookAdapter(stripeWebhookPort, properties);
    mockMvc =
        MockMvcBuilders.standaloneSetup(adapter)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  void POST_webhook_subscription_created_returns_200_and_calls_port() throws Exception {
    var payload = subscriptionPayload("customer.subscription.created", "cus_test_123");
    var sig = stripeSignature(payload, WEBHOOK_SECRET);

    mockMvc
        .perform(
            post("/api/stripe/webhook")
                .contentType(MediaType.TEXT_PLAIN)
                .header("Stripe-Signature", sig)
                .content(payload))
        .andExpect(status().isOk());

    verify(stripeWebhookPort).handleSubscriptionCreated("cus_test_123");
  }

  @Test
  void POST_webhook_subscription_deleted_returns_200_and_calls_port() throws Exception {
    var payload = subscriptionPayload("customer.subscription.deleted", "cus_test_456");
    var sig = stripeSignature(payload, WEBHOOK_SECRET);

    mockMvc
        .perform(
            post("/api/stripe/webhook")
                .contentType(MediaType.TEXT_PLAIN)
                .header("Stripe-Signature", sig)
                .content(payload))
        .andExpect(status().isOk());

    verify(stripeWebhookPort).handleSubscriptionDeleted("cus_test_456");
  }

  @Test
  void POST_webhook_unknown_event_type_returns_200_and_ignores() throws Exception {
    var payload =
        "{\"id\":\"evt_test\",\"object\":\"event\",\"type\":\"payment_intent.succeeded\","
            + "\"data\":{\"object\":{\"id\":\"pi_test\",\"object\":\"payment_intent\"}}}";
    var sig = stripeSignature(payload, WEBHOOK_SECRET);

    mockMvc
        .perform(
            post("/api/stripe/webhook")
                .contentType(MediaType.TEXT_PLAIN)
                .header("Stripe-Signature", sig)
                .content(payload))
        .andExpect(status().isOk());

    verifyNoInteractions(stripeWebhookPort);
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

  private static String subscriptionPayload(String eventType, String customerId) {
    return "{\"id\":\"evt_test\",\"object\":\"event\",\"api_version\":\"2025-01-27.acacia\","
        + "\"type\":\""
        + eventType
        + "\",\"data\":{\"object\":{\"id\":\"sub_test\",\"object\":\"subscription\","
        + "\"customer\":\""
        + customerId
        + "\"}}}";
  }

  private static String stripeSignature(String payload, String secret) throws Exception {
    var timestamp = String.valueOf(Instant.now().getEpochSecond());
    var signedPayload = timestamp + "." + payload;
    var mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    var hashBytes = mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8));
    var hash = HexFormat.of().formatHex(hashBytes);
    return "t=" + timestamp + ",v1=" + hash;
  }
}
