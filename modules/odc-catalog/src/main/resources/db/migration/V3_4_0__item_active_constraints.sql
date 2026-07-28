CREATE UNIQUE INDEX IF NOT EXISTS ux_odc_item_company_sku_active
    ON odc_item (company, UPPER(BTRIM(sku))) WHERE archived = FALSE;
CREATE UNIQUE INDEX IF NOT EXISTS ux_odc_item_company_barcode_active
    ON odc_item (company, UPPER(BTRIM(barcode)))
    WHERE archived = FALSE AND barcode IS NOT NULL AND BTRIM(barcode) <> '';
