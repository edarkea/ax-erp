package com.odc.organization.service;

import com.axelor.auth.db.User;
import com.axelor.i18n.I18n;
import com.google.inject.Inject;
import com.odc.organization.context.ActiveOrganizationContext;
import com.odc.organization.context.CurrentUserProvider;
import com.odc.organization.context.OrganizationContextStore;
import com.odc.organization.db.Branch;
import com.odc.organization.db.Company;
import com.odc.organization.db.UserBranchAccess;
import com.odc.organization.db.UserCompanyAccess;
import com.odc.organization.db.repo.BranchRepository;
import com.odc.organization.db.repo.CompanyRepository;
import com.odc.organization.db.repo.UserBranchAccessRepository;
import com.odc.organization.db.repo.UserCompanyAccessRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ActiveOrganizationServiceImpl implements ActiveOrganizationService {
  private final OrganizationContextStore store;
  private final CurrentUserProvider currentUserProvider;
  private final OrganizationAccessService accessService;
  private final CompanyRepository companyRepository;
  private final BranchRepository branchRepository;
  private final UserCompanyAccessRepository companyAccessRepository;
  private final UserBranchAccessRepository branchAccessRepository;

  @Inject
  public ActiveOrganizationServiceImpl(
      OrganizationContextStore store,
      CurrentUserProvider currentUserProvider,
      OrganizationAccessService accessService,
      CompanyRepository companyRepository,
      BranchRepository branchRepository,
      UserCompanyAccessRepository companyAccessRepository,
      UserBranchAccessRepository branchAccessRepository) {
    this.store = store;
    this.currentUserProvider = currentUserProvider;
    this.accessService = accessService;
    this.companyRepository = companyRepository;
    this.branchRepository = branchRepository;
    this.companyAccessRepository = companyAccessRepository;
    this.branchAccessRepository = branchAccessRepository;
  }

  @Override
  public Optional<Company> getActiveCompany() {
    Optional<User> current = currentUserProvider.getCurrentUser();
    if (current.isEmpty()) {
      store.clear();
      return Optional.empty();
    }
    User user = current.get();
    Optional<Long> storedId = store.getCompanyId();
    if (storedId.isPresent()) {
      Company stored = findCompany(storedId.get());
      if (stored != null && accessService.hasCompanyAccess(user, stored)) return Optional.of(stored);
      store.clear();
    }
    List<Company> defaults = findDefaultCompanies(user);
    if (defaults.size() > 1) throw error("Multiple default companies exist for the user.");
    if (defaults.size() == 1) return Optional.of(store(defaults.get(0)));
    List<Company> available = accessService.findAccessibleCompanies(user);
    if (available.size() == 1) return Optional.of(store(available.get(0)));
    return Optional.empty();
  }

  @Override
  public Company requireActiveCompany() {
    if (currentUserProvider.getCurrentUser().isEmpty()) throw error("No authenticated user exists.");
    Optional<Company> active = getActiveCompany();
    if (active.isPresent()) return active.get();
    if (getAvailableCompanies().isEmpty()) throw error("The user has no enabled companies.");
    throw error("Select a company to continue.");
  }

  @Override
  public Optional<Branch> getActiveBranch() {
    Optional<User> current = currentUserProvider.getCurrentUser();
    if (current.isEmpty()) {
      store.clear();
      return Optional.empty();
    }
    Optional<Company> activeCompany = getActiveCompany();
    if (activeCompany.isEmpty()) {
      store.clearBranchId();
      return Optional.empty();
    }
    User user = current.get();
    Company company = activeCompany.get();
    Optional<Long> storedId = store.getBranchId();
    if (storedId.isPresent()) {
      Branch stored = findBranch(storedId.get());
      if (isCompatible(user, company, stored)) return Optional.of(stored);
      store.clearBranchId();
    }
    List<Branch> defaults = findDefaultBranches(user, company);
    if (defaults.size() > 1) throw error("Multiple default branches exist for the company.");
    if (defaults.size() == 1) return Optional.of(store(defaults.get(0)));
    List<Branch> available = accessService.findAccessibleBranches(user, company);
    if (available.size() == 1) return Optional.of(store(available.get(0)));
    return Optional.empty();
  }

  @Override
  public Branch requireActiveBranch() {
    requireActiveCompany();
    return getActiveBranch().orElseThrow(() -> error("Select a branch to continue."));
  }

  @Override
  public ActiveOrganizationContext getContext() {
    User user =
        currentUserProvider
            .getCurrentUser()
            .orElseThrow(() -> error("No authenticated user exists."));
    Company company = requireActiveCompany();
    return new ActiveOrganizationContext(user, company, getActiveBranch().orElse(null));
  }

  @Override
  public Company setActiveCompany(Long id) {
    Company company = findCompany(id);
    if (company == null) throw error("The selected company is not available.");
    return setActiveCompany(company);
  }

  @Override
  public Company setActiveCompany(Company company) {
    User user = requireUser();
    if (company == null
        || Boolean.TRUE.equals(company.getArchived())
        || !Boolean.TRUE.equals(company.getActive())) {
      throw error("The selected company is not available.");
    }
    accessService.requireCompanyAccess(user, company);
    store.setCompanyId(company.getId());
    Branch stored = store.getBranchId().map(this::findBranch).orElse(null);
    if (!isCompatible(user, company, stored)) store.clearBranchId();
    getActiveBranch();
    return company;
  }

  @Override
  public Branch setActiveBranch(Long id) {
    Branch branch = findBranch(id);
    if (branch == null) throw error("The selected branch is not available.");
    return setActiveBranch(branch);
  }

  @Override
  public Branch setActiveBranch(Branch branch) {
    User user = requireUser();
    Company company = requireActiveCompany();
    if (branch == null
        || Boolean.TRUE.equals(branch.getArchived())
        || !Boolean.TRUE.equals(branch.getActive())) {
      throw error("The selected branch is not available.");
    }
    if (!Objects.equals(id(branch.getCompany()), id(company))) {
      throw error("The branch does not belong to the active company.");
    }
    accessService.requireBranchAccess(user, branch);
    store.setBranchId(branch.getId());
    return branch;
  }

  public void clearActiveCompany() { store.clear(); }
  public void clearActiveBranch() { store.clearBranchId(); }
  public void clearContext() { store.clear(); }
  public List<Company> getAvailableCompanies() {
    return accessService.findAccessibleCompanies(requireUser());
  }
  public List<Branch> getAvailableBranches() {
    return getAvailableBranches(requireActiveCompany());
  }
  public List<Branch> getAvailableBranches(Company company) {
    User user = requireUser();
    accessService.requireCompanyAccess(user, company);
    return accessService.findAccessibleBranches(user, company);
  }

  protected Company findCompany(Long id) { return companyRepository.find(id); }
  protected Branch findBranch(Long id) { return branchRepository.find(id); }
  protected List<Company> findDefaultCompanies(User user) {
    return companyAccessRepository.all()
        .filter("self.user = :user AND self.active = true AND self.archived = false "
            + "AND self.isDefault = true AND self.company.active = true "
            + "AND self.company.archived = false").bind("user", user).fetch().stream()
        .map(UserCompanyAccess::getCompany).toList();
  }
  protected List<Branch> findDefaultBranches(User user, Company company) {
    return branchAccessRepository.all()
        .filter("self.user = :user AND self.branch.company = :company AND self.active = true "
            + "AND self.archived = false AND self.isDefault = true "
            + "AND self.branch.active = true AND self.branch.archived = false")
        .bind("user", user).bind("company", company).fetch().stream()
        .map(UserBranchAccess::getBranch).toList();
  }

  private boolean isCompatible(User user, Company company, Branch branch) {
    return branch != null
        && Objects.equals(id(branch.getCompany()), id(company))
        && accessService.hasBranchAccess(user, branch);
  }
  private Company store(Company company) { store.setCompanyId(company.getId()); return company; }
  private Branch store(Branch branch) { store.setBranchId(branch.getId()); return branch; }
  private User requireUser() {
    return currentUserProvider.getCurrentUser()
        .orElseThrow(() -> error("No authenticated user exists."));
  }
  private Long id(Company company) { return company == null ? null : company.getId(); }
  private IllegalArgumentException error(String message) {
    return new IllegalArgumentException(I18n.get(message));
  }
}
