ALTER TABLE organizations ADD COLUMN stripe_customer_id VARCHAR(255);

CREATE INDEX idx_organizations_stripe_customer_id ON organizations(stripe_customer_id);
