UPDATE odc_point_of_sale SET archived = FALSE WHERE archived IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_odc_point_of_sale_establishment_code_active
  ON odc_point_of_sale (emission_establishment, UPPER(BTRIM(code))) WHERE archived = FALSE;
CREATE UNIQUE INDEX IF NOT EXISTS uq_odc_point_of_sale_establishment_type_default_active
  ON odc_point_of_sale (emission_establishment, type)
  WHERE archived = FALSE AND active = TRUE AND is_default = TRUE;
