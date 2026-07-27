package com.odc.organization.service;

import com.axelor.i18n.I18n;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import com.odc.organization.db.Branch;
import com.odc.organization.db.Company;
import com.odc.organization.db.repo.BranchRepository;
import com.odc.organization.db.repo.CompanyRepository;

public class CompanyServiceImpl implements CompanyService {

  private final CompanyRepository companyRepository;
  private final BranchRepository branchRepository;
  private final OrganizationValidationService validationService;

  @Inject
  public CompanyServiceImpl(
      CompanyRepository companyRepository,
      BranchRepository branchRepository,
      OrganizationValidationService validationService) {
    this.companyRepository = companyRepository;
    this.branchRepository = branchRepository;
    this.validationService = validationService;
  }

  @Override
  @Transactional
  public Company save(Company company) {
    validate(company);
    return persist(company);
  }

  @Override
  public void validate(Company company) {
    if (company == null) {
      throw inconsistency("Company is required.");
    }
    initializeDefaults(company);
    company.setCode(validationService.normalizeRequiredCode(company.getCode(), "Company code"));
    company.setName(validationService.normalizeRequiredName(company.getName(), "Company name"));
    company.setTimezone(validationService.normalizeTimezone(company.getTimezone()));
    company.setLocale(validationService.normalizeLocale(company.getLocale()));
    validationService.requireActive(company.getCountry());

    if (company.getDefaultCurrency() == null && company.getCountry() != null) {
      company.setDefaultCurrency(company.getCountry().getDefaultCurrency());
    }
    validationService.requireActive(company.getDefaultCurrency());

    if (findOtherActive(company.getCode(), company.getId()) != null) {
      throw inconsistency("An active company with code {0} already exists.", company.getCode());
    }
    validateDefaultBranches(company);
    validateStructuralChanges(company);
  }

  @Override
  @Transactional
  public void archive(Company company) {
    if (company == null) {
      throw inconsistency("Company is required.");
    }
    if (hasActiveBranches(company)) {
      throw inconsistency("Company cannot be archived while it has active branches.");
    }
    company.setArchived(true);
    company.setActive(false);
    persist(company);
  }

  @Override
  public void requireUsable(Company company) {
    if (company == null
        || Boolean.TRUE.equals(company.getArchived())
        || !Boolean.TRUE.equals(company.getActive())) {
      throw inconsistency("Company must be active and not archived.");
    }
  }

  protected Company findOtherActive(String code, Long excludedId) {
    if (excludedId == null) {
      return companyRepository
          .all()
          .filter("self.code = :code AND self.archived = false")
          .bind("code", code)
          .fetchOne();
    }
    return companyRepository
        .all()
        .filter("self.code = :code AND self.archived = false AND self.id != :id")
        .bind("code", code)
        .bind("id", excludedId)
        .fetchOne();
  }

  protected boolean hasActiveBranches(Company company) {
    return company.getId() != null
        && branchRepository
                .all()
                .filter("self.company = :company AND self.archived = false")
                .bind("company", company)
                .count()
            > 0;
  }

  protected boolean isStructurallyUsed(Company company) {
    return false;
  }

  private void validateDefaultBranches(Company company) {
    if (company.getBranches() == null) {
      return;
    }
    int defaults = 0;
    for (Branch branch : company.getBranches()) {
      if (!Boolean.TRUE.equals(branch.getArchived())
          && Boolean.TRUE.equals(branch.getActive())
          && Boolean.TRUE.equals(branch.getIsDefault())) {
        defaults++;
      }
    }
    if (defaults > 1) {
      throw inconsistency("Company can have only one active default branch.");
    }
  }

  private void validateStructuralChanges(Company company) {
    if (company.getId() != null && isStructurallyUsed(company)) {
      Company persisted = findPersisted(company.getId());
      if (persisted != null
          && (persisted.getCountry() != company.getCountry()
              || persisted.getDefaultCurrency() != company.getDefaultCurrency())) {
        throw inconsistency("Company structural data cannot be changed after operational use.");
      }
    }
  }

  private void initializeDefaults(Company company) {
    if (company.getArchived() == null) {
      company.setArchived(false);
    }
    if (company.getActive() == null) {
      company.setActive(true);
    }
  }

  protected Company persist(Company company) {
    return companyRepository.save(company);
  }

  protected Company findPersisted(Long id) {
    return companyRepository.find(id);
  }

  private IllegalArgumentException inconsistency(String message, Object... args) {
    String translated = I18n.get(message);
    for (int index = 0; index < args.length; index++) {
      translated = translated.replace("{" + index + "}", String.valueOf(args[index]));
    }
    return new IllegalArgumentException(translated);
  }
}
