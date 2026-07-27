UPDATE odc_currency
SET archived = FALSE
WHERE archived IS NULL;

ALTER TABLE odc_currency
    ALTER COLUMN archived SET DEFAULT FALSE,
    ALTER COLUMN archived SET NOT NULL;
