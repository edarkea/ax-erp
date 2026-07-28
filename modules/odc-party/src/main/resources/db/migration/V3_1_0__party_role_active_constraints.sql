UPDATE odc_party SET archived = FALSE WHERE archived IS NULL;
UPDATE odc_party_role SET archived = FALSE WHERE archived IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_odc_party_company_tax_id_active
    ON odc_party (company, tax_identification_type, UPPER(BTRIM(tax_id)))
    WHERE archived = FALSE AND tax_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_odc_party_role_party_type_active
    ON odc_party_role (party, role_type)
    WHERE archived = FALSE;
