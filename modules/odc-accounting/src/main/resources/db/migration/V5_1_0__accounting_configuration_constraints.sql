UPDATE odc_chart_account SET archived = FALSE WHERE archived IS NULL;
UPDATE odc_accounting_role_definition SET archived = FALSE WHERE archived IS NULL;
UPDATE odc_accounting_setup_entry SET archived = FALSE WHERE archived IS NULL;

ALTER TABLE odc_chart_account
  ALTER COLUMN archived SET DEFAULT FALSE,
  ALTER COLUMN archived SET NOT NULL;
ALTER TABLE odc_accounting_role_definition
  ALTER COLUMN archived SET DEFAULT FALSE,
  ALTER COLUMN archived SET NOT NULL;
ALTER TABLE odc_accounting_setup_entry
  ALTER COLUMN archived SET DEFAULT FALSE,
  ALTER COLUMN archived SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_odc_chart_account_company_code_active
  ON odc_chart_account (company, UPPER(BTRIM(code)))
  WHERE archived = FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS ux_odc_accounting_role_definition_code_active
  ON odc_accounting_role_definition (UPPER(BTRIM(code)))
  WHERE archived = FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS ux_odc_accounting_setup_entry_active
  ON odc_accounting_setup_entry (
    company,
    document_group,
    COALESCE(document_type, ''),
    accounting_role_definition,
    COALESCE(branch, -1),
    COALESCE(currency, -1)
  )
  WHERE archived = FALSE;
