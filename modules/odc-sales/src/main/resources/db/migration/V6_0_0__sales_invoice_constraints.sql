UPDATE odc_sales_invoice SET archived = FALSE WHERE archived IS NULL;
UPDATE odc_sales_invoice_line SET archived = FALSE WHERE archived IS NULL;
ALTER TABLE odc_sales_invoice ALTER COLUMN archived SET DEFAULT FALSE;
ALTER TABLE odc_sales_invoice ALTER COLUMN archived SET NOT NULL;
ALTER TABLE odc_sales_invoice_line ALTER COLUMN archived SET DEFAULT FALSE;
ALTER TABLE odc_sales_invoice_line ALTER COLUMN archived SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_odc_sales_invoice_document_no
  ON odc_sales_invoice (company, document_no) WHERE document_no IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_odc_sales_invoice_reservation
  ON odc_sales_invoice (document_sequence_reservation) WHERE document_sequence_reservation IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_odc_sales_invoice_line_sequence_active
  ON odc_sales_invoice_line (sales_invoice, sequence) WHERE archived = FALSE;
CREATE INDEX IF NOT EXISTS idx_odc_sales_invoice_company_date ON odc_sales_invoice (company, invoice_date);
CREATE INDEX IF NOT EXISTS idx_odc_sales_invoice_company_status ON odc_sales_invoice (company, status);
