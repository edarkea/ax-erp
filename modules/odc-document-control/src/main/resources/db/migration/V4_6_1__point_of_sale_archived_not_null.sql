UPDATE odc_point_of_sale
SET archived = FALSE
WHERE archived IS NULL;

ALTER TABLE odc_point_of_sale
  ALTER COLUMN archived SET DEFAULT FALSE,
  ALTER COLUMN archived SET NOT NULL;
