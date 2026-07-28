package com.odc.document.service;

import com.axelor.auth.db.User;
import com.odc.document.db.PointOfSale;
import com.odc.document.db.UserPointAssignment;
import java.util.Optional;

public interface UserPointAssignmentService {
  UserPointAssignment save(UserPointAssignment assignment);
  void validate(UserPointAssignment assignment);
  Optional<UserPointAssignment> findActiveAssignment(User user, PointOfSale pointOfSale);
  boolean hasUserAccess(User user, PointOfSale pointOfSale);
  void requireUserAccess(User user, PointOfSale pointOfSale);
  void requireCurrentUserAccess(PointOfSale pointOfSale);
}
