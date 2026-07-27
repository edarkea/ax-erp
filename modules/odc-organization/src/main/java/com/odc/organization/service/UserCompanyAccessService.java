package com.odc.organization.service;

import com.odc.organization.db.UserCompanyAccess;

public interface UserCompanyAccessService {
  UserCompanyAccess save(UserCompanyAccess access);
  void validate(UserCompanyAccess access);
  void setDefault(UserCompanyAccess access);
  void activate(UserCompanyAccess access);
  void deactivate(UserCompanyAccess access, boolean allowWithoutDefault);
  void archive(UserCompanyAccess access, boolean allowWithoutDefault);
}
