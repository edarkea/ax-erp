package com.odc.organization.service;

import com.odc.organization.context.ActiveOrganizationContext;
import com.odc.organization.db.Branch;
import com.odc.organization.db.Company;
import java.util.List;
import java.util.Optional;

public interface ActiveOrganizationService {
  Optional<Company> getActiveCompany();
  Company requireActiveCompany();
  Optional<Branch> getActiveBranch();
  Branch requireActiveBranch();
  ActiveOrganizationContext getContext();
  Company setActiveCompany(Company company);
  Company setActiveCompany(Long companyId);
  Branch setActiveBranch(Branch branch);
  Branch setActiveBranch(Long branchId);
  void clearActiveCompany();
  void clearActiveBranch();
  void clearContext();
  List<Company> getAvailableCompanies();
  List<Branch> getAvailableBranches();
  List<Branch> getAvailableBranches(Company company);
}
