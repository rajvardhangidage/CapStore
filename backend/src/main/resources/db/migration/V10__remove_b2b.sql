-- The store is retail (B2C) only. Business accounts become regular customer
-- accounts; bulk price tiers, quote requests and business fields are removed.
UPDATE users SET role = 'USER' WHERE role = 'B2B_USER';

DROP TABLE quote_requests;
DROP TABLE bulk_price_tiers;

ALTER TABLE users
  DROP COLUMN company_name,
  DROP COLUMN gst_number,
  DROP COLUMN business_approved;
