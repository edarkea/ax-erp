UPDATE odc_document_sequence_reservation SET archived = FALSE WHERE archived IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_odc_document_reservation_sequence
  ON odc_document_sequence_reservation (document_series, sequence_number);
CREATE UNIQUE INDEX IF NOT EXISTS uq_odc_document_reservation_number
  ON odc_document_sequence_reservation (document_series, document_no);
CREATE UNIQUE INDEX IF NOT EXISTS uq_odc_document_reservation_correlation
  ON odc_document_sequence_reservation (document_series, document_model, correlation_key)
  WHERE correlation_key IS NOT NULL;
