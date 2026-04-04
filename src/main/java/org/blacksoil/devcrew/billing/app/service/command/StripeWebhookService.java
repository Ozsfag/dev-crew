package org.blacksoil.devcrew.billing.app.service.command;

import lombok.RequiredArgsConstructor;
import org.blacksoil.devcrew.billing.domain.StripeWebhookPort;
import org.blacksoil.devcrew.organization.app.service.command.OrganizationCommandService;
import org.blacksoil.devcrew.organization.domain.OrgPlan;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StripeWebhookService implements StripeWebhookPort {

  private final OrganizationCommandService organizationCommandService;

  @Override
  @Transactional
  public void handleSubscriptionCreated(String stripeCustomerId) {
    organizationCommandService.updatePlanByStripeCustomer(stripeCustomerId, OrgPlan.PRO);
  }

  @Override
  @Transactional
  public void handleSubscriptionDeleted(String stripeCustomerId) {
    organizationCommandService.updatePlanByStripeCustomer(stripeCustomerId, OrgPlan.FREE);
  }
}
