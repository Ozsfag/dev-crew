package org.blacksoil.devcrew.billing.app.service.command;

import static org.mockito.Mockito.verify;

import org.blacksoil.devcrew.organization.app.service.command.OrganizationCommandService;
import org.blacksoil.devcrew.organization.domain.OrgPlan;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StripeWebhookServiceTest {

  @Mock private OrganizationCommandService organizationCommandService;

  @InjectMocks private StripeWebhookService service;

  @Test
  void handleSubscriptionCreated_upgrades_org_plan_to_pro() {
    service.handleSubscriptionCreated("cus_test123");

    verify(organizationCommandService).updatePlanByStripeCustomer("cus_test123", OrgPlan.PRO);
  }

  @Test
  void handleSubscriptionDeleted_downgrades_org_plan_to_free() {
    service.handleSubscriptionDeleted("cus_test123");

    verify(organizationCommandService).updatePlanByStripeCustomer("cus_test123", OrgPlan.FREE);
  }
}
