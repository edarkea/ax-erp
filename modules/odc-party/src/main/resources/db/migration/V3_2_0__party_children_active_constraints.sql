CREATE UNIQUE INDEX IF NOT EXISTS ux_odc_party_tag_company_name_active
    ON odc_party_tag (company, UPPER(BTRIM(name))) WHERE archived = FALSE;
CREATE UNIQUE INDEX IF NOT EXISTS ux_odc_party_tag_link_active
    ON odc_party_tag_link (party, tag) WHERE archived = FALSE;
CREATE UNIQUE INDEX IF NOT EXISTS ux_odc_party_contact_primary_active
    ON odc_party_contact_point (party, type)
    WHERE archived = FALSE AND active = TRUE AND is_primary = TRUE;
CREATE UNIQUE INDEX IF NOT EXISTS ux_odc_party_address_billing_default_active
    ON odc_party_address (party)
    WHERE archived = FALSE AND active = TRUE AND is_billing_default = TRUE;
CREATE UNIQUE INDEX IF NOT EXISTS ux_odc_party_address_shipping_default_active
    ON odc_party_address (party)
    WHERE archived = FALSE AND active = TRUE AND is_shipping_default = TRUE;
