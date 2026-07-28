UPDATE odc_accounting_period SET archived = FALSE WHERE archived IS NULL;

ALTER TABLE odc_accounting_period
  ALTER COLUMN archived SET DEFAULT FALSE,
  ALTER COLUMN archived SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_odc_accounting_period_company_code_active
  ON odc_accounting_period (company, UPPER(BTRIM(code)))
  WHERE archived = FALSE;
