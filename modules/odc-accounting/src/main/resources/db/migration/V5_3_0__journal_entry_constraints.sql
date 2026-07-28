UPDATE odc_journal_entry SET archived = false WHERE archived IS NULL;
ALTER TABLE odc_journal_entry ALTER COLUMN archived SET DEFAULT false;
ALTER TABLE odc_journal_entry ALTER COLUMN archived SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_odc_journal_entry_company_date
  ON odc_journal_entry (company, accounting_date);
CREATE INDEX IF NOT EXISTS idx_odc_journal_entry_period
  ON odc_journal_entry (accounting_period);
CREATE INDEX IF NOT EXISTS idx_odc_journal_entry_company_status
  ON odc_journal_entry (company, status);
CREATE INDEX IF NOT EXISTS idx_odc_journal_entry_source
  ON odc_journal_entry (company, source_model, source_id)
  WHERE source_model IS NOT NULL AND source_id IS NOT NULL;
