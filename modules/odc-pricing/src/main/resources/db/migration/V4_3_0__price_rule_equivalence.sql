DROP INDEX IF EXISTS ux_odc_price_list_item_active;
CREATE UNIQUE INDEX IF NOT EXISTS uq_odc_price_list_item_active_rule
  ON odc_price_list_item (
    price_list,
    item,
    COALESCE(minimum_quantity, -1),
    COALESCE(maximum_quantity, -1),
    COALESCE(valid_from, DATE '0001-01-01'),
    COALESCE(valid_until, DATE '9999-12-31')
  )
  WHERE archived = FALSE;
