UPDATE odc_user_company_access SET archived = FALSE WHERE archived IS NULL;
UPDATE odc_user_company_access SET active = TRUE WHERE active IS NULL;
UPDATE odc_user_company_access SET is_default = FALSE WHERE is_default IS NULL;
UPDATE odc_user_branch_access SET archived = FALSE WHERE archived IS NULL;
UPDATE odc_user_branch_access SET active = TRUE WHERE active IS NULL;
UPDATE odc_user_branch_access SET is_default = FALSE WHERE is_default IS NULL;
UPDATE odc_user_preference SET archived = FALSE WHERE archived IS NULL;

ALTER TABLE odc_user_company_access ALTER COLUMN archived SET DEFAULT FALSE,
  ALTER COLUMN archived SET NOT NULL, ALTER COLUMN active SET DEFAULT TRUE,
  ALTER COLUMN active SET NOT NULL, ALTER COLUMN is_default SET DEFAULT FALSE,
  ALTER COLUMN is_default SET NOT NULL;
ALTER TABLE odc_user_branch_access ALTER COLUMN archived SET DEFAULT FALSE,
  ALTER COLUMN archived SET NOT NULL, ALTER COLUMN active SET DEFAULT TRUE,
  ALTER COLUMN active SET NOT NULL, ALTER COLUMN is_default SET DEFAULT FALSE,
  ALTER COLUMN is_default SET NOT NULL;
ALTER TABLE odc_user_preference ALTER COLUMN archived SET DEFAULT FALSE,
  ALTER COLUMN archived SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_odc_user_company_access_active
  ON odc_user_company_access (user_id, company) WHERE archived = FALSE;
CREATE UNIQUE INDEX IF NOT EXISTS uk_odc_user_default_company_active
  ON odc_user_company_access (user_id)
  WHERE archived = FALSE AND active = TRUE AND is_default = TRUE;
CREATE UNIQUE INDEX IF NOT EXISTS uk_odc_user_branch_access_active
  ON odc_user_branch_access (user_id, branch) WHERE archived = FALSE;
CREATE UNIQUE INDEX IF NOT EXISTS uk_odc_user_preference_active
  ON odc_user_preference (user_id) WHERE archived = FALSE;
