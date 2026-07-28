package com.odc.document.service;

import com.axelor.i18n.I18n;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import com.odc.document.db.UserPointAssignment;
import com.odc.document.db.repo.UserPointAssignmentRepository;
import com.odc.organization.db.Branch;
import com.odc.organization.db.Company;
import com.odc.organization.db.repo.UserBranchAccessRepository;
import com.odc.organization.db.repo.UserCompanyAccessRepository;
import com.odc.organization.service.AccessValidationService;

public class UserPointAssignmentServiceImpl implements UserPointAssignmentService {
  private final UserPointAssignmentRepository repository;
  private final UserCompanyAccessRepository companyAccessRepository;
  private final UserBranchAccessRepository branchAccessRepository;
  private final EmissionConfigurationService configurationService;
  private final AccessValidationService accessValidationService;

  @Inject
  public UserPointAssignmentServiceImpl(
      UserPointAssignmentRepository repository,
      UserCompanyAccessRepository companyAccessRepository,
      UserBranchAccessRepository branchAccessRepository,
      EmissionConfigurationService configurationService,
      AccessValidationService accessValidationService) {
    this.repository = repository;
    this.companyAccessRepository = companyAccessRepository;
    this.branchAccessRepository = branchAccessRepository;
    this.configurationService = configurationService;
    this.accessValidationService = accessValidationService;
  }

  @Override @Transactional
  public UserPointAssignment save(UserPointAssignment value) {
    validate(value);
    return repository.save(value);
  }

  @Override
  public void validate(UserPointAssignment value) {
    if (value == null || value.getUser() == null || value.getPointOfSale() == null)
      throw error("User and point of sale are required.");
    accessValidationService.requireUsable(value.getUser());
    configurationService.requireUsable(value.getPointOfSale());
    if (value.getArchived() == null) value.setArchived(false);
    if (value.getActive() == null) value.setActive(true);
    if (value.getIsDefault() == null) value.setIsDefault(false);
    Branch branch = value.getPointOfSale().getEmissionEstablishment().getBranch();
    Company company = branch.getCompany();
    if (!hasCompanyAccess(value, company) || !hasBranchAccess(value, branch))
      throw error("User has no access to the point company or branch.");
    String filter =
        "self.user = :user AND self.pointOfSale = :point AND self.archived = false";
    var query = repository.all().filter(filter).bind("user", value.getUser())
        .bind("point", value.getPointOfSale());
    if (value.getId() != null) query = repository.all().filter(filter + " AND self.id != :id")
        .bind("user", value.getUser()).bind("point", value.getPointOfSale())
        .bind("id", value.getId());
    if (query.fetchOne() != null) throw error("User is already assigned to this point.");
    if (Boolean.TRUE.equals(value.getIsDefault()) && otherDefault(value, company) != null)
      throw error("User already has a default point for this company and type.");
  }

  protected boolean hasCompanyAccess(UserPointAssignment value, Company company) {
    return companyAccessRepository.all()
        .filter("self.user = :user AND self.company = :company AND self.active = true "
            + "AND self.archived = false")
        .bind("user", value.getUser()).bind("company", company).count() > 0;
  }
  protected boolean hasBranchAccess(UserPointAssignment value, Branch branch) {
    return branchAccessRepository.all()
        .filter("self.user = :user AND self.branch = :branch AND self.active = true "
            + "AND self.archived = false")
        .bind("user", value.getUser()).bind("branch", branch).count() > 0;
  }
  protected UserPointAssignment otherDefault(UserPointAssignment value, Company company) {
    String filter =
        "self.user = :user AND self.isDefault = true AND self.active = true "
            + "AND self.archived = false AND "
            + "self.pointOfSale.emissionEstablishment.branch.company = :company "
            + "AND self.pointOfSale.type = :type";
    var query = repository.all().filter(filter).bind("user", value.getUser())
        .bind("company", company).bind("type", value.getPointOfSale().getType());
    if (value.getId() != null) query = repository.all().filter(filter + " AND self.id != :id")
        .bind("user", value.getUser()).bind("company", company)
        .bind("type", value.getPointOfSale().getType()).bind("id", value.getId());
    return query.fetchOne();
  }
  private IllegalArgumentException error(String message) {
    return new IllegalArgumentException(I18n.get(message));
  }
}
