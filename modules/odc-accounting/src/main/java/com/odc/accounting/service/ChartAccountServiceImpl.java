package com.odc.accounting.service;

import com.axelor.auth.AuthUtils;
import com.axelor.i18n.I18n;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import com.odc.accounting.db.ChartAccount;
import com.odc.accounting.db.repo.AccountingSetupEntryRepository;
import com.odc.accounting.db.repo.ChartAccountRepository;
import com.odc.organization.db.Company;
import com.odc.organization.service.ActiveOrganizationService;
import com.odc.organization.service.OrganizationAccessService;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class ChartAccountServiceImpl implements ChartAccountService {
  private final ChartAccountRepository repository;
  private final AccountingSetupEntryRepository setupRepository;
  private final ActiveOrganizationService activeOrganizationService;
  private final OrganizationAccessService accessService;

  @Inject
  public ChartAccountServiceImpl(
      ChartAccountRepository repository,
      AccountingSetupEntryRepository setupRepository,
      ActiveOrganizationService activeOrganizationService,
      OrganizationAccessService accessService) {
    this.repository = repository;
    this.setupRepository = setupRepository;
    this.activeOrganizationService = activeOrganizationService;
    this.accessService = accessService;
  }

  @Override @Transactional
  public ChartAccount save(ChartAccount account) {
    validate(account);
    return persist(account);
  }

  @Override
  public void validate(ChartAccount account) {
    if (account == null) throw error("Account is required.");
    Company active = activeOrganizationService.requireActiveCompany();
    if (account.getId() == null) account.setCompany(active);
    requireCompany(account.getCompany(), active);
    defaults(account);
    account.setCode(required(account.getCode(), "Account code is required.").toUpperCase());
    account.setName(required(account.getName(), "Account name is required."));
    if (account.getAccountType() == null) throw error("Account type is required.");
    if (account.getNormalBalance() == null) account.setNormalBalance(defaultBalance(account));
    if (account.getNormalBalance() == null) throw error("Normal balance is required.");
    if (account.getSequence() < 0) throw error("Sequence cannot be negative.");
    validateHierarchy(account);
    if (!Boolean.TRUE.equals(account.getArchived()) && findDuplicate(account) != null)
      throw error("Account code already exists.");
    if (Boolean.TRUE.equals(account.getIsPosting()) && hasActiveChildren(account))
      throw error("A posting account cannot have child accounts.");
    ChartAccount persisted = findPersisted(account.getId());
    if (persisted != null && !same(persisted.getCompany(), account.getCompany()))
      throw error("Account company cannot be changed.");
    if (persisted != null && !Objects.equals(persisted.getCode(), account.getCode())
        && hasActiveSetupEntries(account))
      throw error("Account code cannot be changed after accounting configuration use.");
  }

  @Override
  public void validateHierarchy(ChartAccount account) {
    ChartAccount parent = account.getParent();
    if (parent == null) return;
    requireBasicUsable(parent);
    if (!same(account.getCompany(), parent.getCompany()))
      throw error("Parent account belongs to another company.");
    if (sameRecord(account, parent)) throw error("An account cannot be its own parent.");
    if (!Objects.equals(account.getAccountType(), parent.getAccountType()))
      throw error("Child account type must match parent account type.");
    if (Boolean.TRUE.equals(parent.getIsPosting()))
      throw error("A posting account cannot have child accounts.");
    Set<Long> ids = new HashSet<>();
    Set<ChartAccount> objects = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    ChartAccount cursor = parent;
    while (cursor != null) {
      if (!objects.add(cursor)
          || cursor == account
          || (account.getId() != null && account.getId().equals(cursor.getId()))
          || (cursor.getId() != null && !ids.add(cursor.getId())))
        throw error("Chart account hierarchy contains a cycle.");
      cursor = cursor.getParent();
    }
  }

  @Override @Transactional
  public void archive(ChartAccount account) {
    requirePersisted(account);
    if (hasActiveChildren(account)) throw error("Cannot archive an account with active children.");
    if (hasActiveSetupEntries(account))
      throw error("Cannot archive an account used by an active accounting setup.");
    account.setActive(false); account.setArchived(true); persist(account);
  }

  @Override @Transactional
  public ChartAccount restore(ChartAccount account) {
    requirePersisted(account);
    account.setArchived(false); account.setActive(true); validate(account);
    return persist(account);
  }

  @Override
  public void requireUsable(ChartAccount account) {
    requireBasicUsable(account);
    Company active = activeOrganizationService.requireActiveCompany();
    requireCompany(account.getCompany(), active);
  }

  @Override
  public void requirePostingAccount(ChartAccount account) {
    requireUsable(account);
    if (!Boolean.TRUE.equals(account.getIsPosting()))
      throw error("Selected account does not allow postings.");
  }

  protected ChartAccount findDuplicate(ChartAccount account) {
    String filter = "self.company = :company AND self.code = :code AND self.archived = false";
    var query = repository.all().filter(filter).bind("company", account.getCompany())
        .bind("code", account.getCode());
    if (account.getId() != null) query = repository.all().filter(filter + " AND self.id != :id")
        .bind("company", account.getCompany()).bind("code", account.getCode())
        .bind("id", account.getId());
    return query.fetchOne();
  }
  protected boolean hasActiveChildren(ChartAccount account) {
    if (account.getId() == null) return account.getChildren() != null
        && account.getChildren().stream().anyMatch(this::isActive);
    return repository.all().filter("self.parent = :parent AND self.active = true "
        + "AND self.archived = false").bind("parent", account).count() > 0;
  }
  protected boolean hasActiveSetupEntries(ChartAccount account) {
    if (account.getId() == null) return false;
    return setupRepository.all().filter("self.account = :account AND self.active = true "
        + "AND self.archived = false").bind("account", account).count() > 0;
  }
  protected ChartAccount findPersisted(Long id) { return id == null ? null : repository.find(id); }
  protected ChartAccount persist(ChartAccount value) { return repository.save(value); }

  private void requireCompany(Company company, Company active) {
    if (company == null || Boolean.TRUE.equals(company.getArchived())
        || !Boolean.TRUE.equals(company.getActive())) throw error("Company must be active.");
    if (!same(company, active)) throw error("You do not have access to the account company.");
    accessService.requireCompanyAccess(AuthUtils.getUser(), company);
  }
  private void requireBasicUsable(ChartAccount account) {
    if (!isActive(account)) throw error("Account must be active and not archived.");
  }
  private boolean isActive(ChartAccount value) {
    return value != null && !Boolean.TRUE.equals(value.getArchived())
        && Boolean.TRUE.equals(value.getActive());
  }
  private void defaults(ChartAccount value) {
    if (value.getArchived() == null) value.setArchived(false);
    if (value.getActive() == null) value.setActive(true);
    if (value.getIsPosting() == null) value.setIsPosting(false);
    if (value.getSequence() == null) value.setSequence(0);
  }
  private String defaultBalance(ChartAccount value) {
    return switch (value.getAccountType()) {
      case "ASSET", "EXPENSE" -> "DEBIT";
      case "LIABILITY", "EQUITY", "INCOME" -> "CREDIT";
      default -> null;
    };
  }
  private String required(String value, String message) {
    if (value == null || value.trim().isEmpty()) throw error(message);
    return value.trim();
  }
  private boolean same(com.axelor.db.Model left, com.axelor.db.Model right) {
    return left == right || left != null && right != null && left.getId() != null
        && Objects.equals(left.getId(), right.getId());
  }
  private boolean sameRecord(ChartAccount left, ChartAccount right) { return same(left, right); }
  private void requirePersisted(ChartAccount value) {
    if (value == null || value.getId() == null) throw error("Account is required.");
  }
  private IllegalArgumentException error(String message) {
    return new IllegalArgumentException(I18n.get(message));
  }
}
