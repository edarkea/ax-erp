UPDATE odc_emission_establishment SET archived = FALSE WHERE archived IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_odc_emission_establishment_branch_code_active
  ON odc_emission_establishment (branch, UPPER(BTRIM(code))) WHERE archived = FALSE;
CREATE UNIQUE INDEX IF NOT EXISTS uq_odc_emission_establishment_branch_default_active
  ON odc_emission_establishment (branch)
  WHERE archived = FALSE AND active = TRUE AND is_default = TRUE;
