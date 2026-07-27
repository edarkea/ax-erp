UPDATE odc_country SET archived = FALSE WHERE archived IS NULL;
UPDATE odc_state SET archived = FALSE WHERE archived IS NULL;
UPDATE odc_city SET archived = FALSE WHERE archived IS NULL;

ALTER TABLE odc_country
    ALTER COLUMN archived SET DEFAULT FALSE,
    ALTER COLUMN archived SET NOT NULL;

ALTER TABLE odc_state
    ALTER COLUMN archived SET DEFAULT FALSE,
    ALTER COLUMN archived SET NOT NULL;

ALTER TABLE odc_city
    ALTER COLUMN archived SET DEFAULT FALSE,
    ALTER COLUMN archived SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_odc_country_active_code
    ON odc_country (UPPER(BTRIM(code)))
    WHERE archived = FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS uk_odc_state_active_country_code
    ON odc_state (country, UPPER(BTRIM(code)))
    WHERE archived = FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS uk_odc_city_active_state_code
    ON odc_city (state, UPPER(BTRIM(code)))
    WHERE archived = FALSE;
