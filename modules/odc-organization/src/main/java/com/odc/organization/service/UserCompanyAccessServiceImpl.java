package com.odc.organization.service;

import com.axelor.auth.db.User;
import com.axelor.db.JPA;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import com.odc.organization.db.Company;
import com.odc.organization.db.UserCompanyAccess;
import com.odc.organization.db.repo.UserBranchAccessRepository;
import com.odc.organization.db.repo.UserCompanyAccessRepository;
import jakarta.persistence.LockModeType;

public class UserCompanyAccessServiceImpl implements UserCompanyAccessService {

  private final UserCompanyAccessRepository repository;
  private final UserBranchAccessRepository branchAccessRepository;
  private final CompanyService companyService;
  private final AccessValidationService validation;

  @Inject
  public UserCompanyAccessServiceImpl(
      UserCompanyAccessRepository repository,
      UserBranchAccessRepository branchAccessRepository,
      CompanyService companyService,
      AccessValidationService validation) {
    this.repository = repository;
    this.branchAccessRepository = branchAccessRepository;
    this.companyService = companyService;
    this.validation = validation;
  }

  @Override
  @Transactional
  public UserCompanyAccess save(UserCompanyAccess access) {
    validate(access);
    return persist(access);
  }

  @Override
  public void validate(UserCompanyAccess access) {
    if (access == null) throw validation.error("Company access is required.");
    initialize(access);
    validation.requireUsable(access.getUser());
    if (access.getCompany() == null) throw validation.error("Company is required.");
    companyService.requireUsable(access.getCompany());
    if ((!Boolean.TRUE.equals(access.getActive()) || Boolean.TRUE.equals(access.getArchived()))
        && Boolean.TRUE.equals(access.getIsDefault())) {
      throw validation.error("An inactive or archived access cannot be default.");
    }
    if (findOther(access.getUser(), access.getCompany(), access.getId()) != null) {
      throw validation.error("The user already has access to this company.");
    }
    if (Boolean.TRUE.equals(access.getIsDefault())
        && findOtherDefault(access.getUser(), access.getId()) != null) {
      throw validation.error("The user already has a default company.");
    }
  }

  @Override
  @Transactional
  public void setDefault(UserCompanyAccess access) {
    validateRequiredActive(access);
    lockUser(access.getUser());
    UserCompanyAccess current = findOtherDefault(access.getUser(), access.getId());
    if (current != null) {
      current.setIsDefault(false);
      persist(current);
    }
    access.setIsDefault(true);
    persist(access);
  }

  @Override
  @Transactional
  public void activate(UserCompanyAccess access) {
    access.setArchived(false);
    access.setActive(true);
    access.setIsDefault(false);
    validate(access);
    persist(access);
  }

  @Override
  @Transactional
  public void deactivate(UserCompanyAccess access, boolean allowWithoutDefault) {
    requireDefaultResolution(access, allowWithoutDefault);
    if (hasActiveBranchAccesses(access)) {
      throw validation.error("Company access cannot be disabled while branch accesses are active.");
    }
    access.setActive(false);
    access.setIsDefault(false);
    persist(access);
  }

  @Override
  @Transactional
  public void archive(UserCompanyAccess access, boolean allowWithoutDefault) {
    deactivate(access, allowWithoutDefault);
    access.setArchived(true);
    persist(access);
  }

  protected UserCompanyAccess findOther(User user, Company company, Long id) {
    var query =
        repository
            .all()
            .filter(
                "self.user = :user AND self.company = :company AND self.archived = false"
                    + (id == null ? "" : " AND self.id != :id"))
            .bind("user", user)
            .bind("company", company);
    if (id != null) query.bind("id", id);
    return query.fetchOne();
  }

  protected UserCompanyAccess findOtherDefault(User user, Long id) {
    var query =
        repository
            .all()
            .filter(
                "self.user = :user AND self.archived = false AND self.active = true "
                    + "AND self.isDefault = true"
                    + (id == null ? "" : " AND self.id != :id"))
            .bind("user", user);
    if (id != null) query.bind("id", id);
    return query.fetchOne();
  }

  protected boolean hasActiveBranchAccesses(UserCompanyAccess access) {
    return branchAccessRepository
            .all()
            .filter(
                "self.user = :user AND self.branch.company = :company "
                    + "AND self.archived = false AND self.active = true")
            .bind("user", access.getUser())
            .bind("company", access.getCompany())
            .count()
        > 0;
  }

  protected void lockUser(User user) {
    JPA.em().lock(user, LockModeType.PESSIMISTIC_WRITE);
  }

  protected UserCompanyAccess persist(UserCompanyAccess access) {
    return repository.save(access);
  }

  private void requireDefaultResolution(UserCompanyAccess access, boolean allowed) {
    if (Boolean.TRUE.equals(access.getIsDefault()) && !allowed) {
      throw validation.error("Default company access must be replaced or explicitly cleared.");
    }
  }

  private void validateRequiredActive(UserCompanyAccess access) {
    validate(access);
    if (!Boolean.TRUE.equals(access.getActive()) || Boolean.TRUE.equals(access.getArchived())) {
      throw validation.error("Company access must be active.");
    }
  }

  private void initialize(UserCompanyAccess access) {
    if (access.getArchived() == null) access.setArchived(false);
    if (access.getActive() == null) access.setActive(true);
    if (access.getIsDefault() == null) access.setIsDefault(false);
  }
}
