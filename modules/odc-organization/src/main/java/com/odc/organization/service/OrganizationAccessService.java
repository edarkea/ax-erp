package com.odc.organization.service;

import com.axelor.auth.db.User;
import com.odc.organization.db.Branch;
import com.odc.organization.db.Company;
import java.util.List;

public interface OrganizationAccessService {
  List<Company> findAccessibleCompanies(User user);
  List<Branch> findAccessibleBranches(User user, Company company);
  boolean hasCompanyAccess(User user, Company company);
  boolean hasBranchAccess(User user, Branch branch);
  void requireCompanyAccess(User user, Company company);
  void requireBranchAccess(User user, Branch branch);
  void grantCompanyAccess(User user, Company company);
  void revokeCompanyAccess(User user, Company company);
  void grantBranchAccess(User user, Branch branch);
  void revokeBranchAccess(User user, Branch branch);
}
