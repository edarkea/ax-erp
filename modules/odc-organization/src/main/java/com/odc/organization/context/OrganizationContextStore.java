package com.odc.organization.context;

import java.util.Optional;

public interface OrganizationContextStore {
  Optional<Long> getCompanyId();
  void setCompanyId(Long companyId);
  void clearCompanyId();
  Optional<Long> getBranchId();
  void setBranchId(Long branchId);
  void clearBranchId();
  void clear();
}
