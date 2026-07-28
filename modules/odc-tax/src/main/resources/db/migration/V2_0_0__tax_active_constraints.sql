UPDATE odc_tax_category SET archived = FALSE WHERE archived IS NULL;
UPDATE odc_tax_rate SET archived = FALSE WHERE archived IS NULL;

ALTER TABLE odc_tax_category
    ALTER COLUMN archived SET DEFAULT FALSE,
    ALTER COLUMN archived SET NOT NULL;

ALTER TABLE odc_tax_rate
    ALTER COLUMN archived SET DEFAULT FALSE,
    ALTER COLUMN archived SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_odc_tax_category_country_type_code_active
    ON odc_tax_category (country, type, UPPER(BTRIM(code)))
    WHERE archived = FALSE;

CREATE INDEX IF NOT EXISTS ix_odc_tax_rate_category_validity_active
    ON odc_tax_rate (tax_category, valid_from, valid_until)
    WHERE archived = FALSE;
