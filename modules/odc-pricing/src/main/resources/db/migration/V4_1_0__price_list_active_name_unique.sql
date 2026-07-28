UPDATE odc_price_list SET archived = FALSE WHERE archived IS NULL;

ALTER TABLE odc_price_list
    ALTER COLUMN archived SET DEFAULT FALSE,
    ALTER COLUMN archived SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_odc_price_list_company_name_active
    ON odc_price_list (company, UPPER(BTRIM(name)))
    WHERE archived = FALSE;
