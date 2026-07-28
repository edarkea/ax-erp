UPDATE odc_price_list_item SET archived = FALSE WHERE archived IS NULL;

ALTER TABLE odc_price_list_item
    ALTER COLUMN archived SET DEFAULT FALSE,
    ALTER COLUMN archived SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_odc_price_list_item_active
    ON odc_price_list_item (price_list, item)
    WHERE archived = FALSE;
