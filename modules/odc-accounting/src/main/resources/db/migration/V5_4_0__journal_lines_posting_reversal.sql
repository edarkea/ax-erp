UPDATE odc_journal_line SET archived = FALSE WHERE archived IS NULL;
ALTER TABLE odc_journal_line ALTER COLUMN archived SET DEFAULT FALSE;
ALTER TABLE odc_journal_line ALTER COLUMN archived SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_odc_journal_line_entry_sequence_active
  ON odc_journal_line (journal_entry, sequence)
  WHERE archived = FALSE;
CREATE INDEX IF NOT EXISTS idx_odc_journal_line_entry
  ON odc_journal_line (journal_entry);
CREATE INDEX IF NOT EXISTS idx_odc_journal_line_account
  ON odc_journal_line (account);
CREATE INDEX IF NOT EXISTS idx_odc_journal_line_party
  ON odc_journal_line (party);

CREATE UNIQUE INDEX IF NOT EXISTS ux_odc_journal_entry_posting_sequence
  ON odc_journal_entry (company, posting_year, posting_sequence)
  WHERE posting_sequence IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_odc_journal_entry_number
  ON odc_journal_entry (company, entry_number)
  WHERE entry_number IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_odc_journal_entry_reversal_of
  ON odc_journal_entry (reversal_of)
  WHERE reversal_of IS NOT NULL AND archived = FALSE;
