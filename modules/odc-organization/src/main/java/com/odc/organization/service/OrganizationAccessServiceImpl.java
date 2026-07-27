package com.odc.organization.service;

import com.axelor.auth.db.User;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import com.odc.organization.db.Branch;
import com.odc.organization.db.Company;
import com.odc.organization.db.UserBranchAccess;
import com.odc.organization.db.UserCompanyAccess;
import com.odc.organization.db.repo.UserBranchAccessRepository;
import com.odc.organization.db.repo.UserCompanyAccessRepository;
import java.util.List;

public class OrganizationAccessServiceImpl implements OrganizationAccessService {

  private final UserCompanyAccessRepository companyRepository;
  private final UserBranchAccessRepository branchRepository;
  private final UserCompanyAccessService companyAccessService;
  private final UserBranchAccessService branchAccessService;
  private final AccessValidationService validation;

  @Inject
  public OrganizationAccessServiceImpl(
      UserCompanyAccessRepository companyRepository,
      UserBranchAccessRepository branchRepository,
      UserCompanyAccessService companyAccessService,
      UserBranchAccessService branchAccessService,
      AccessValidationService validation) {
    this.companyRepository = companyRepository;
    this.branchRepository = branchRepository;
    this.companyAccessService = companyAccessService;
    this.branchAccessService = branchAccessService;
    this.validation = validation;
  }

  @Override
  public List<Company> findAccessibleCompanies(User user) {
    validation.requireUsable(user);
    return companyRepository
        .all()
        .filter(
            "self.user = :user AND self.archived = false AND self.active = true "
                + "AND self.company.archived = false AND self.company.active = true")
        .bind("user", user)
        .fetch()
        .stream()
        .map(UserCompanyAccess::getCompany)
        .toList();
  }

  @Override
  public List<Branch> findAccessibleBranches(User user, Company company) {
    requireCompanyAccess(user, company);
    return branchRepository
        .all()
        .filter(
            "self.user = :user AND self.branch.company = :company "
                + "AND self.archived = false AND self.active = true "
                + "AND self.branch.archived = false AND self.branch.active = true")
        .bind("user", user)
        .bind("company", company)
        .fetch()
        .stream()
        .map(UserBranchAccess::getBranch)
        .toList();
  }

  @Override
  public boolean hasCompanyAccess(User user, Company company) {
    return user != null
        && company != null
        && companyRepository
                .all()
                .filter(
                    "self.user = :user AND self.company = :company "
                        + "AND self.archived = false AND self.active = true "
                        + "AND self.company.archived = false AND self.company.active = true")
                .bind("user", user)
                .bind("company", company)
                .count()
            > 0;
  }

  @Override
  public boolean hasBranchAccess(User user, Branch branch) {
    return user != null
        && branch != null
        && branchRepository
                .all()
                .filter(
                    "self.user = :user AND self.branch = :branch "
                        + "AND self.archived = false AND self.active = true "
                        + "AND self.branch.archived = false AND self.branch.active = true")
                .bind("user", user)
                .bind("branch", branch)
                .count()
            > 0
        && hasCompanyAccess(user, branch.getCompany());
  }

  @Override
  public void requireCompanyAccess(User user, Company company) {
    if (!hasCompanyAccess(user, company)) {
      throw validation.error("User has no active access to the company.");
    }
  }

  @Override
  public void requireBranchAccess(User user, Branch branch) {
    if (!hasBranchAccess(user, branch)) {
      throw validation.error("User has no active access to the branch.");
    }
  }

  @Override
  @Transactional
  public void grantCompanyAccess(User user, Company company) {
    UserCompanyAccess access = new UserCompanyAccess();
    access.setUser(user);
    access.setCompany(company);
    companyAccessService.save(access);
  }

  @Override
  @Transactional
  public void revokeCompanyAccess(User user, Company company) {
    UserCompanyAccess access =
        companyRepository
            .all()
            .filter("self.user = :user AND self.company = :company AND self.archived = false")
            .bind("user", user)
            .bind("company", company)
            .fetchOne();
    if (access != null) companyAccessService.archive(access, false);
  }

  @Override
  @Transactional
  public void grantBranchAccess(User user, Branch branch) {
    UserBranchAccess access = new UserBranchAccess();
    access.setUser(user);
    access.setBranch(branch);
    branchAccessService.save(access);
  }

  @Override
  @Transactional
  public void revokeBranchAccess(User user, Branch branch) {
    UserBranchAccess access =
        branchRepository
            .all()
            .filter("self.user = :user AND self.branch = :branch AND self.archived = false")
            .bind("user", user)
            .bind("branch", branch)
            .fetchOne();
    if (access != null) branchAccessService.archive(access, false);
  }
}
