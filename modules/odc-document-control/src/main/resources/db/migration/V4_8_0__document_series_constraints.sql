UPDATE odc_document_series SET archived = FALSE WHERE archived IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_odc_document_series_context_active
  ON odc_document_series (emission_establishment, point_of_sale, document_type)
  WHERE archived = FALSE;
