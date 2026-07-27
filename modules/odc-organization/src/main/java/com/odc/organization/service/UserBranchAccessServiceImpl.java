package com.odc.organization.service;

import com.axelor.auth.db.User;
import com.axelor.db.JPA;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import com.odc.organization.db.Branch;
import com.odc.organization.db.Company;
import com.odc.organization.db.UserBranchAccess;
import com.odc.organization.db.UserCompanyAccess;
import com.odc.organization.db.repo.UserBranchAccessRepository;
import com.odc.organization.db.repo.UserCompanyAccessRepository;
import jakarta.persistence.LockModeType;

public class UserBranchAccessServiceImpl implements UserBranchAccessService {

  private final UserBranchAccessRepository repository;
  private final UserCompanyAccessRepository companyAccessRepository;
  private final BranchService branchService;
  private final AccessValidationService validation;

  @Inject
  public UserBranchAccessServiceImpl(
      UserBranchAccessRepository repository,
      UserCompanyAccessRepository companyAccessRepository,
      BranchService branchService,
      AccessValidationService validation) {
    this.repository = repository;
    this.companyAccessRepository = companyAccessRepository;
    this.branchService = branchService;
    this.validation = validation;
  }

  @Override
  @Transactional
  public UserBranchAccess save(UserBranchAccess access) {
    validate(access);
    return persist(access);
  }

  @Override
  public void validate(UserBranchAccess access) {
    if (access == null) throw validation.error("Branch access is required.");
    initialize(access);
    validation.requireUsable(access.getUser());
    if (access.getBranch() == null) throw validation.error("Branch is required.");
    branchService.requireUsable(access.getBranch());
    if (findCompanyAccess(access.getUser(), access.getBranch().getCompany()) == null) {
      throw validation.error("The user has no active access to the branch company.");
    }
    if ((!Boolean.TRUE.equals(access.getActive()) || Boolean.TRUE.equals(access.getArchived()))
        && Boolean.TRUE.equals(access.getIsDefault())) {
      throw validation.error("An inactive or archived access cannot be default.");
    }
    if (findOther(access.getUser(), access.getBranch(), access.getId()) != null) {
      throw validation.error("The user already has access to this branch.");
    }
    if (Boolean.TRUE.equals(access.getIsDefault())
        && findOtherDefault(
                access.getUser(), access.getBranch().getCompany(), access.getId())
            != null) {
      throw validation.error("The user already has a default branch for this company.");
    }
  }

  @Override
  @Transactional
  public void setDefault(UserBranchAccess access) {
    validate(access);
    UserCompanyAccess companyAccess =
        findCompanyAccess(access.getUser(), access.getBranch().getCompany());
    lockCompanyAccess(companyAccess);
    UserBranchAccess current =
        findOtherDefault(access.getUser(), access.getBranch().getCompany(), access.getId());
    if (current != null) {
      current.setIsDefault(false);
      persist(current);
    }
    access.setIsDefault(true);
    persist(access);
  }

  @Override
  @Transactional
  public void activate(UserBranchAccess access) {
    access.setArchived(false);
    access.setActive(true);
    access.setIsDefault(false);
    validate(access);
    persist(access);
  }

  @Override
  @Transactional
  public void deactivate(UserBranchAccess access, boolean allowWithoutDefault) {
    if (Boolean.TRUE.equals(access.getIsDefault()) && !allowWithoutDefault) {
      throw validation.error("Default branch access must be replaced or explicitly cleared.");
    }
    access.setActive(false);
    access.setIsDefault(false);
    persist(access);
  }

  @Override
  @Transactional
  public void archive(UserBranchAccess access, boolean allowWithoutDefault) {
    deactivate(access, allowWithoutDefault);
    access.setArchived(true);
    persist(access);
  }

  protected UserCompanyAccess findCompanyAccess(User user, Company company) {
    return companyAccessRepository
        .all()
        .filter(
            "self.user = :user AND self.company = :company "
                + "AND self.archived = false AND self.active = true")
        .bind("user", user)
        .bind("company", company)
        .fetchOne();
  }

  protected UserBranchAccess findOther(User user, Branch branch, Long id) {
    var query =
        repository
            .all()
            .filter(
                "self.user = :user AND self.branch = :branch AND self.archived = false"
                    + (id == null ? "" : " AND self.id != :id"))
            .bind("user", user)
            .bind("branch", branch);
    if (id != null) query.bind("id", id);
    return query.fetchOne();
  }

  protected UserBranchAccess findOtherDefault(User user, Company company, Long id) {
    var query =
        repository
            .all()
            .filter(
                "self.user = :user AND self.branch.company = :company "
                    + "AND self.archived = false AND self.active = true AND self.isDefault = true"
                    + (id == null ? "" : " AND self.id != :id"))
            .bind("user", user)
            .bind("company", company);
    if (id != null) query.bind("id", id);
    return query.fetchOne();
  }

  protected void lockCompanyAccess(UserCompanyAccess access) {
    JPA.em().lock(access, LockModeType.PESSIMISTIC_WRITE);
  }

  protected UserBranchAccess persist(UserBranchAccess access) {
    return repository.save(access);
  }

  private void initialize(UserBranchAccess access) {
    if (access.getArchived() == null) access.setArchived(false);
    if (access.getActive() == null) access.setActive(true);
    if (access.getIsDefault() == null) access.setIsDefault(false);
  }
}
