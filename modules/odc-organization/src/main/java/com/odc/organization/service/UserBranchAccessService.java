package com.odc.organization.service;

import com.odc.organization.db.UserBranchAccess;

public interface UserBranchAccessService {
  UserBranchAccess save(UserBranchAccess access);
  void validate(UserBranchAccess access);
  void setDefault(UserBranchAccess access);
  void activate(UserBranchAccess access);
  void deactivate(UserBranchAccess access, boolean allowWithoutDefault);
  void archive(UserBranchAccess access, boolean allowWithoutDefault);
}
