UPDATE odc_company SET archived = FALSE WHERE archived IS NULL;
UPDATE odc_company SET active = TRUE WHERE active IS NULL;
UPDATE odc_branch SET archived = FALSE WHERE archived IS NULL;
UPDATE odc_branch SET active = TRUE WHERE active IS NULL;
UPDATE odc_branch SET is_default = FALSE WHERE is_default IS NULL;

ALTER TABLE odc_company
    ALTER COLUMN archived SET DEFAULT FALSE,
    ALTER COLUMN archived SET NOT NULL,
    ALTER COLUMN active SET DEFAULT TRUE,
    ALTER COLUMN active SET NOT NULL;

ALTER TABLE odc_branch
    ALTER COLUMN archived SET DEFAULT FALSE,
    ALTER COLUMN archived SET NOT NULL,
    ALTER COLUMN active SET DEFAULT TRUE,
    ALTER COLUMN active SET NOT NULL,
    ALTER COLUMN is_default SET DEFAULT FALSE,
    ALTER COLUMN is_default SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_odc_company_active_code
    ON odc_company (UPPER(BTRIM(code)))
    WHERE archived = FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS uk_odc_branch_active_company_code
    ON odc_branch (company, UPPER(BTRIM(code)))
    WHERE archived = FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS uk_odc_branch_active_company_default
    ON odc_branch (company)
    WHERE archived = FALSE AND active = TRUE AND is_default = TRUE;
