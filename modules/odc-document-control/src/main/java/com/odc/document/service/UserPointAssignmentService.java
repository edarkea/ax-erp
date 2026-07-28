package com.odc.document.service;

import com.odc.document.db.UserPointAssignment;

public interface UserPointAssignmentService {
  UserPointAssignment save(UserPointAssignment assignment);
  void validate(UserPointAssignment assignment);
}
