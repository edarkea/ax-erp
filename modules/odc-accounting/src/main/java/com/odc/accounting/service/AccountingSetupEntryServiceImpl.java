package com.odc.accounting.service;

import com.axelor.auth.AuthUtils;
import com.axelor.i18n.I18n;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import com.odc.accounting.db.AccountingSetupEntry;
import com.odc.accounting.db.repo.AccountingSetupEntryRepository;
import com.odc.organization.db.Branch;
import com.odc.organization.db.Company;
import com.odc.organization.service.ActiveOrganizationService;
import com.odc.organization.service.OrganizationAccessService;
import com.odc.reference.db.Currency;
import java.util.List;
import java.util.Objects;

public class AccountingSetupEntryServiceImpl implements AccountingSetupEntryService {
  private final AccountingSetupEntryRepository repository;
  private final ActiveOrganizationService activeOrganizationService;
  private final OrganizationAccessService accessService;
  private final ChartAccountService accountService;
  private final AccountingRoleDefinitionService roleService;
  private final AccountingConfigurationValidationService validationService;

  @Inject
  public AccountingSetupEntryServiceImpl(
      AccountingSetupEntryRepository repository,
      ActiveOrganizationService activeOrganizationService,
      OrganizationAccessService accessService,
      ChartAccountService accountService,
      AccountingRoleDefinitionService roleService,
      AccountingConfigurationValidationService validationService) {
    this.repository = repository;
    this.activeOrganizationService = activeOrganizationService;
    this.accessService = accessService;
    this.accountService = accountService;
    this.roleService = roleService;
    this.validationService = validationService;
  }

  @Override @Transactional
  public AccountingSetupEntry save(AccountingSetupEntry entry) {
    validate(entry); return persist(entry);
  }

  @Override
  public void validate(AccountingSetupEntry entry) {
    if (entry == null) throw error("Accounting setup entry is required.");
    Company active = activeOrganizationService.requireActiveCompany();
    if (entry.getId() == null) entry.setCompany(active);
    requireCompany(entry.getCompany(), active);
    defaults(entry);
    roleService.requireUsable(entry.getAccountingRoleDefinition());
    accountService.requirePostingAccount(entry.getAccount());
    if (!same(entry.getCompany(), entry.getAccount().getCompany()))
      throw error("Account belongs to another company.");
    validateBranch(entry.getBranch(), entry.getCompany());
    validateCurrency(entry.getCurrency());
    validationService.validateRoleContext(
        entry.getDocumentGroup(), entry.getDocumentType(),
        entry.getAccountingRoleDefinition());
    validationService.validateAccountCompatibility(
        entry.getAccountingRoleDefinition(), entry.getAccount());
    if (entry.getPriority() < 0) throw error("Priority cannot be negative.");
    if (!Boolean.TRUE.equals(entry.getArchived()) && findExactDuplicate(entry) != null)
      throw error("Accounting setup already exists.");
    if (!Boolean.TRUE.equals(entry.getArchived()) && findAmbiguous(entry) != null)
      throw error("Accounting setup creates an ambiguous resolution.");
    AccountingSetupEntry persisted = findPersisted(entry.getId());
    if (persisted != null && !same(persisted.getCompany(), entry.getCompany()))
      throw error("Accounting setup company cannot be changed.");
  }

  @Override @Transactional
  public void archive(AccountingSetupEntry entry) {
    requirePersisted(entry); entry.setActive(false); entry.setArchived(true); persist(entry);
  }
  @Override @Transactional
  public AccountingSetupEntry restore(AccountingSetupEntry entry) {
    requirePersisted(entry); entry.setArchived(false); entry.setActive(true);
    validate(entry); return persist(entry);
  }
  @Override
  public void requireUsable(AccountingSetupEntry entry) {
    if (entry == null || Boolean.TRUE.equals(entry.getArchived())
        || !Boolean.TRUE.equals(entry.getActive()))
      throw error("Accounting setup must be active and not archived.");
    validate(entry);
  }

  protected AccountingSetupEntry findExactDuplicate(AccountingSetupEntry entry) {
    return activePeers(entry).stream().filter(other -> exactContext(entry, other)).findFirst().orElse(null);
  }
  protected AccountingSetupEntry findAmbiguous(AccountingSetupEntry entry) {
    int score = specificity(entry);
    return activePeers(entry).stream()
        .filter(other -> specificity(other) == score)
        .filter(other -> Objects.equals(other.getPriority(), entry.getPriority()))
        .filter(other -> overlaps(entry.getDocumentType(), other.getDocumentType())
            && overlaps(entry.getBranch(), other.getBranch())
            && overlaps(entry.getCurrency(), other.getCurrency()))
        .findFirst().orElse(null);
  }
  protected List<AccountingSetupEntry> activePeers(AccountingSetupEntry entry) {
    String filter = "self.company = :company AND self.documentGroup = :group "
        + "AND self.accountingRoleDefinition = :role AND self.active = true "
        + "AND self.archived = false";
    var query = repository.all().filter(filter).bind("company", entry.getCompany())
        .bind("group", entry.getDocumentGroup())
        .bind("role", entry.getAccountingRoleDefinition());
    List<AccountingSetupEntry> values = query.fetch();
    return values.stream().filter(value -> entry.getId() == null
        || !entry.getId().equals(value.getId())).toList();
  }
  protected AccountingSetupEntry findPersisted(Long id) {
    return id == null ? null : repository.find(id);
  }
  protected AccountingSetupEntry persist(AccountingSetupEntry entry) {
    return repository.save(entry);
  }
  private boolean exactContext(AccountingSetupEntry a, AccountingSetupEntry b) {
    return Objects.equals(a.getDocumentType(), b.getDocumentType())
        && sameNullable(a.getBranch(), b.getBranch())
        && sameNullable(a.getCurrency(), b.getCurrency());
  }
  private boolean overlaps(String a, String b) { return a == null || b == null || a.equals(b); }
  private boolean overlaps(com.axelor.db.Model a, com.axelor.db.Model b) {
    return a == null || b == null || same(a, b);
  }
  private boolean sameNullable(com.axelor.db.Model a, com.axelor.db.Model b) {
    return a == null ? b == null : same(a, b);
  }
  private int specificity(AccountingSetupEntry value) {
    return (value.getDocumentType() == null ? 0 : 4)
        + (value.getBranch() == null ? 0 : 2)
        + (value.getCurrency() == null ? 0 : 1);
  }
  private void validateBranch(Branch branch, Company company) {
    if (branch == null) return;
    if (Boolean.TRUE.equals(branch.getArchived()) || !Boolean.TRUE.equals(branch.getActive()))
      throw error("Branch must be active.");
    if (!same(branch.getCompany(), company)) throw error("Branch belongs to another company.");
    accessService.requireBranchAccess(AuthUtils.getUser(), branch);
  }
  private void validateCurrency(Currency currency) {
    if (currency != null && Boolean.TRUE.equals(currency.getArchived()))
      throw error("Currency is archived.");
  }
  private void requireCompany(Company company, Company active) {
    if (company == null || Boolean.TRUE.equals(company.getArchived())
        || !Boolean.TRUE.equals(company.getActive())) throw error("Company must be active.");
    if (!same(company, active)) throw error("You do not have access to the setup company.");
    accessService.requireCompanyAccess(AuthUtils.getUser(), company);
  }
  private void defaults(AccountingSetupEntry value) {
    if (value.getArchived() == null) value.setArchived(false);
    if (value.getActive() == null) value.setActive(true);
    if (value.getPriority() == null) value.setPriority(100);
  }
  private boolean same(com.axelor.db.Model left, com.axelor.db.Model right) {
    return left == right || left != null && right != null && left.getId() != null
        && Objects.equals(left.getId(), right.getId());
  }
  private void requirePersisted(AccountingSetupEntry value) {
    if (value == null || value.getId() == null) throw error("Accounting setup entry is required.");
  }
  private IllegalArgumentException error(String message) {
    return new IllegalArgumentException(I18n.get(message));
  }
}
