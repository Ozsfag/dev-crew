package org.blacksoil.devcrew.billing.app.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(BillingProperties.class)
public class BillingConfig {}
