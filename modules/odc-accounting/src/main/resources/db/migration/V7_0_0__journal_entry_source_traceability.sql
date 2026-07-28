DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'odc_journal_entry' AND column_name = 'source_id'
  ) AND NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'odc_journal_entry' AND column_name = 'source_record_id'
  ) THEN
    ALTER TABLE odc_journal_entry RENAME COLUMN source_id TO source_record_id;
  ELSIF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'odc_journal_entry' AND column_name = 'source_id'
  ) THEN
    UPDATE odc_journal_entry
      SET source_record_id = source_id
      WHERE source_record_id IS NULL;
    ALTER TABLE odc_journal_entry DROP COLUMN source_id;
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'odc_journal_entry' AND column_name = 'source_reference'
  ) AND NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'odc_journal_entry' AND column_name = 'source_document_no'
  ) THEN
    ALTER TABLE odc_journal_entry RENAME COLUMN source_reference TO source_document_no;
  ELSIF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'odc_journal_entry' AND column_name = 'source_reference'
  ) THEN
    UPDATE odc_journal_entry
      SET source_document_no = source_reference
      WHERE source_document_no IS NULL;
    ALTER TABLE odc_journal_entry DROP COLUMN source_reference;
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS ix_odc_journal_entry_source_lookup
  ON odc_journal_entry (company, source_model, source_record_id)
  WHERE source_model IS NOT NULL AND source_record_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_odc_journal_entry_primary_source
  ON odc_journal_entry (company, source_model, source_record_id)
  WHERE source_model IS NOT NULL AND source_record_id IS NOT NULL
    AND reversal_of IS NULL AND archived = FALSE;

CREATE INDEX IF NOT EXISTS ix_odc_journal_entry_source_document_no
  ON odc_journal_entry (company, source_model, source_document_no)
  WHERE source_document_no IS NOT NULL;
