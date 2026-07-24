CREATE UNIQUE INDEX IF NOT EXISTS uk_odc_currency_active_code
    ON odc_currency (UPPER(BTRIM(code)))
    WHERE archived = FALSE;
