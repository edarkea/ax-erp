UPDATE odc_user_point_assignment SET archived = FALSE WHERE archived IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_odc_user_point_assignment_active
  ON odc_user_point_assignment (user_id, point_of_sale) WHERE archived = FALSE;
