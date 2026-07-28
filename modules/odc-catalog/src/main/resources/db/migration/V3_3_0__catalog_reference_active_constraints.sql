CREATE UNIQUE INDEX IF NOT EXISTS ux_odc_unit_of_measure_code_active
    ON odc_unit_of_measure (UPPER(BTRIM(code))) WHERE archived = FALSE;
CREATE UNIQUE INDEX IF NOT EXISTS ux_odc_item_category_company_code_active
    ON odc_item_category (company, UPPER(BTRIM(code))) WHERE archived = FALSE;
