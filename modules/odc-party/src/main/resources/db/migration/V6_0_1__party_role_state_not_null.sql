UPDATE odc_party_role SET archived = FALSE WHERE archived IS NULL;
UPDATE odc_party_role SET active = TRUE WHERE active IS NULL;

ALTER TABLE odc_party_role
  ALTER COLUMN archived SET DEFAULT FALSE,
  ALTER COLUMN archived SET NOT NULL,
  ALTER COLUMN active SET DEFAULT TRUE,
  ALTER COLUMN active SET NOT NULL;
