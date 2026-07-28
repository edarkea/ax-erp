package com.odc.accounting.service;

import com.axelor.auth.db.User;
import com.odc.accounting.db.AccountingRoleDefinition;
import com.odc.accounting.db.AccountingSetupEntry;
import com.odc.accounting.db.ChartAccount;
import com.odc.organization.context.ActiveOrganizationContext;
import com.odc.organization.db.Branch;
import com.odc.organization.db.Company;
import com.odc.organization.service.ActiveOrganizationService;
import com.odc.organization.service.OrganizationAccessService;
import com.odc.reference.db.Currency;
import java.util.List;
import java.util.Optional;

final class AccountingTestSupport {
  private AccountingTestSupport() {}
  static Company company(long id) {
    Company value = new Company(); value.setId(id); value.setActive(true); value.setArchived(false);
    return value;
  }
  static Branch branch(long id, Company company) {
    Branch value = new Branch(); value.setId(id); value.setCompany(company);
    value.setActive(true); value.setArchived(false); return value;
  }
  static Currency currency(long id) {
    Currency value = new Currency(); value.setId(id); value.setArchived(false); return value;
  }
  static ChartAccount account(long id, Company company, String code, boolean posting) {
    ChartAccount value = new ChartAccount(); value.setId(id); value.setCompany(company);
    value.setCode(code); value.setName(code); value.setAccountType("ASSET");
    value.setNormalBalance("DEBIT"); value.setIsPosting(posting);
    value.setSequence(0); value.setActive(true); value.setArchived(false); return value;
  }
  static AccountingRoleDefinition role(long id, String code, String group, String side) {
    AccountingRoleDefinition value = new AccountingRoleDefinition();
    value.setId(id); value.setCode(code); value.setName(code); value.setDocumentGroup(group);
    value.setSideHint(side); value.setActive(true); value.setArchived(false); return value;
  }
  static AccountingSetupEntry setup(
      long id, Company company, AccountingRoleDefinition role, ChartAccount account) {
    AccountingSetupEntry value = new AccountingSetupEntry();
    value.setId(id); value.setCompany(company); value.setDocumentGroup(role.getDocumentGroup());
    value.setAccountingRoleDefinition(role); value.setAccount(account); value.setPriority(100);
    value.setActive(true); value.setArchived(false); return value;
  }

  static class ActiveStub implements ActiveOrganizationService {
    Company company;
    ActiveStub(Company company) { this.company = company; }
    public Optional<Company> getActiveCompany() { return Optional.ofNullable(company); }
    public Company requireActiveCompany() {
      if (company == null) throw new IllegalArgumentException("No active company");
      return company;
    }
    public Optional<Branch> getActiveBranch() { return Optional.empty(); }
    public Branch requireActiveBranch() { throw new IllegalArgumentException(); }
    public ActiveOrganizationContext getContext() { return null; }
    public Company setActiveCompany(Company value) { company = value; return value; }
    public Company setActiveCompany(Long id) { return company; }
    public Branch setActiveBranch(Branch value) { return value; }
    public Branch setActiveBranch(Long id) { return null; }
    public void clearActiveCompany() { company = null; }
    public void clearActiveBranch() {}
    public void clearContext() { company = null; }
    public List<Company> getAvailableCompanies() { return List.of(company); }
    public List<Branch> getAvailableBranches() { return List.of(); }
    public List<Branch> getAvailableBranches(Company company) { return List.of(); }
  }

  static class AccessStub implements OrganizationAccessService {
    boolean allowed = true;
    public List<Company> findAccessibleCompanies(User user) { return List.of(); }
    public List<Branch> findAccessibleBranches(User user, Company company) { return List.of(); }
    public boolean hasCompanyAccess(User user, Company company) { return allowed; }
    public boolean hasBranchAccess(User user, Branch branch) { return allowed; }
    public void requireCompanyAccess(User user, Company company) {
      if (!allowed) throw new IllegalArgumentException("No access");
    }
    public void requireBranchAccess(User user, Branch branch) {
      if (!allowed) throw new IllegalArgumentException("No access");
    }
    public void grantCompanyAccess(User user, Company company) {}
    public void revokeCompanyAccess(User user, Company company) {}
    public void grantBranchAccess(User user, Branch branch) {}
    public void revokeBranchAccess(User user, Branch branch) {}
  }
}
