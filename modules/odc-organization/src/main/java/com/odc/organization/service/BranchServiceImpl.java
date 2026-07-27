package com.odc.organization.service;

import com.axelor.i18n.I18n;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import com.odc.organization.db.Branch;
import com.odc.organization.db.Company;
import com.odc.organization.db.repo.BranchRepository;

public class BranchServiceImpl implements BranchService {

  private final BranchRepository branchRepository;
  private final CompanyService companyService;
  private final OrganizationValidationService validationService;

  @Inject
  public BranchServiceImpl(
      BranchRepository branchRepository,
      CompanyService companyService,
      OrganizationValidationService validationService) {
    this.branchRepository = branchRepository;
    this.companyService = companyService;
    this.validationService = validationService;
  }

  @Override
  @Transactional
  public Branch save(Branch branch) {
    validate(branch);
    return persist(branch);
  }

  @Override
  public void validate(Branch branch) {
    validate(branch, null);
  }

  @Override
  public void validate(Branch branch, Company owningCompany) {
    if (branch == null) {
      throw inconsistency("Branch is required.");
    }
    initializeDefaults(branch);
    branch.setCode(validationService.normalizeRequiredCode(branch.getCode(), "Branch code"));
    branch.setName(validationService.normalizeRequiredName(branch.getName(), "Branch name"));

    Company company = owningCompany != null ? owningCompany : branch.getCompany();
    if (company == null) {
      throw inconsistency("Branch company is required.");
    }
    companyService.requireUsable(company);
    validationService.requireActive(branch.getCity());

    if (findOtherActive(company, branch.getCode(), branch.getId()) != null) {
      throw inconsistency(
          "An active branch with code {0} already exists in company {1}.",
          branch.getCode(), company.getCode());
    }
    if (isActiveDefault(branch)
        && findOtherActiveDefault(company, branch.getId()) != null) {
      throw inconsistency("Company already has an active default branch.");
    }
    validateCompanyChange(branch);
  }

  @Override
  @Transactional
  public void setDefault(Branch branch) {
    if (branch == null || branch.getCompany() == null) {
      throw inconsistency("Branch company is required.");
    }
    requireUsable(branch);

    Branch current = findOtherActiveDefault(branch.getCompany(), branch.getId());
    if (current != null) {
      current.setIsDefault(false);
      persist(current);
    }
    branch.setIsDefault(true);
    persist(branch);
  }

  @Override
  @Transactional
  public void archive(Branch branch) {
    if (branch == null) {
      throw inconsistency("Branch is required.");
    }
    if (Boolean.TRUE.equals(branch.getIsDefault()) && Boolean.TRUE.equals(branch.getActive())) {
      throw inconsistency("Default branch must be replaced before it can be archived.");
    }
    if (requiresActiveBranch(branch.getCompany()) && countOtherActive(branch) == 0) {
      throw inconsistency("The last active branch of the company cannot be archived.");
    }
    branch.setArchived(true);
    branch.setActive(false);
    persist(branch);
  }

  @Override
  public void requireUsable(Branch branch) {
    if (branch == null
        || Boolean.TRUE.equals(branch.getArchived())
        || !Boolean.TRUE.equals(branch.getActive())) {
      throw inconsistency("Branch must be active and not archived.");
    }
    companyService.requireUsable(branch.getCompany());
  }

  protected Branch findOtherActive(Company company, String code, Long excludedId) {
    if (excludedId == null) {
      return branchRepository
          .all()
          .filter("self.company = :company AND self.code = :code AND self.archived = false")
          .bind("company", company)
          .bind("code", code)
          .fetchOne();
    }
    return branchRepository
        .all()
        .filter(
            "self.company = :company AND self.code = :code "
                + "AND self.archived = false AND self.id != :id")
        .bind("company", company)
        .bind("code", code)
        .bind("id", excludedId)
        .fetchOne();
  }

  protected Branch findOtherActiveDefault(Company company, Long excludedId) {
    if (excludedId == null) {
      return branchRepository
          .all()
          .filter(
              "self.company = :company AND self.archived = false "
                  + "AND self.active = true AND self.isDefault = true")
          .bind("company", company)
          .fetchOne();
    }
    return branchRepository
        .all()
        .filter(
            "self.company = :company AND self.archived = false "
                + "AND self.active = true AND self.isDefault = true AND self.id != :id")
        .bind("company", company)
        .bind("id", excludedId)
        .fetchOne();
  }

  protected long countOtherActive(Branch branch) {
    if (branch.getCompany() == null) {
      return 0;
    }
    if (branch.getId() == null) {
      return branchRepository
          .all()
          .filter("self.company = :company AND self.archived = false AND self.active = true")
          .bind("company", branch.getCompany())
          .count();
    }
    return branchRepository
        .all()
        .filter(
            "self.company = :company AND self.archived = false "
                + "AND self.active = true AND self.id != :id")
        .bind("company", branch.getCompany())
        .bind("id", branch.getId())
        .count();
  }

  protected boolean requiresActiveBranch(Company company) {
    return false;
  }

  protected boolean isOperationallyUsed(Branch branch) {
    return false;
  }

  private void validateCompanyChange(Branch branch) {
    if (branch.getId() == null || !isOperationallyUsed(branch)) {
      return;
    }
    Branch persisted = findPersisted(branch.getId());
    if (persisted != null && persisted.getCompany() != branch.getCompany()) {
      throw inconsistency("Branch company cannot be changed after operational use.");
    }
  }

  private boolean isActiveDefault(Branch branch) {
    return !Boolean.TRUE.equals(branch.getArchived())
        && Boolean.TRUE.equals(branch.getActive())
        && Boolean.TRUE.equals(branch.getIsDefault());
  }

  private void initializeDefaults(Branch branch) {
    if (branch.getArchived() == null) {
      branch.setArchived(false);
    }
    if (branch.getActive() == null) {
      branch.setActive(true);
    }
    if (branch.getIsDefault() == null) {
      branch.setIsDefault(false);
    }
  }

  protected Branch persist(Branch branch) {
    return branchRepository.save(branch);
  }

  protected Branch findPersisted(Long id) {
    return branchRepository.find(id);
  }

  private IllegalArgumentException inconsistency(String message, Object... args) {
    String translated = I18n.get(message);
    for (int index = 0; index < args.length; index++) {
      translated = translated.replace("{" + index + "}", String.valueOf(args[index]));
    }
    return new IllegalArgumentException(translated);
  }
}
