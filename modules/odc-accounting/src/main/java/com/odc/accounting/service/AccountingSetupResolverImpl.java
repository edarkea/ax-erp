package com.odc.accounting.service;

import com.axelor.auth.AuthUtils;
import com.axelor.i18n.I18n;
import com.google.inject.Inject;
import com.odc.accounting.db.AccountingRoleDefinition;
import com.odc.accounting.db.AccountingSetupEntry;
import com.odc.accounting.db.repo.AccountingSetupEntryRepository;
import com.odc.organization.db.Branch;
import com.odc.organization.db.Company;
import com.odc.organization.service.ActiveOrganizationService;
import com.odc.organization.service.OrganizationAccessService;
import com.odc.reference.db.Currency;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class AccountingSetupResolverImpl implements AccountingSetupResolver {
  private final AccountingSetupEntryRepository repository;
  private final ActiveOrganizationService activeOrganizationService;
  private final OrganizationAccessService accessService;
  private final AccountingRoleDefinitionService roleService;
  private final AccountingConfigurationValidationService validationService;

  @Inject
  public AccountingSetupResolverImpl(
      AccountingSetupEntryRepository repository,
      ActiveOrganizationService activeOrganizationService,
      OrganizationAccessService accessService,
      AccountingRoleDefinitionService roleService,
      AccountingConfigurationValidationService validationService) {
    this.repository = repository;
    this.activeOrganizationService = activeOrganizationService;
    this.accessService = accessService;
    this.roleService = roleService;
    this.validationService = validationService;
  }

  @Override
  public Optional<AccountingSetupResolution> findAccount(
      Company company, Branch branch, Currency currency, String group,
      String type, AccountingRoleDefinition role) {
    validateInput(company, branch, currency, group, type, role);
    List<AccountingSetupEntry> candidates = findCandidates(company, group, role).stream()
        .filter(entry -> matches(entry, branch, currency, type))
        .filter(entry -> entry.getAccount() != null
            && !Boolean.TRUE.equals(entry.getAccount().getArchived())
            && Boolean.TRUE.equals(entry.getAccount().getActive())
            && Boolean.TRUE.equals(entry.getAccount().getIsPosting()))
        .sorted(Comparator.comparingInt(this::specificity).reversed()
            .thenComparing(AccountingSetupEntry::getPriority))
        .toList();
    if (candidates.isEmpty()) return Optional.empty();
    AccountingSetupEntry winner = candidates.get(0);
    if (candidates.size() > 1
        && specificity(winner) == specificity(candidates.get(1))
        && Objects.equals(winner.getPriority(), candidates.get(1).getPriority()))
      throw error("Multiple accounting setups apply with the same priority.");
    return Optional.of(new AccountingSetupResolution(
        winner, winner.getAccount(), role, company, branch, currency, specificity(winner)));
  }

  @Override
  public AccountingSetupResolution requireAccount(
      Company company, Branch branch, Currency currency, String group,
      String type, AccountingRoleDefinition role) {
    return findAccount(company, branch, currency, group, type, role)
        .orElseThrow(() -> error("No account is configured for the selected accounting role."));
  }

  @Override
  public AccountingSetupResolution requireAccount(
      AccountingPostingContext context, AccountingRoleDefinition role) {
    if (context == null) throw error("Accounting posting context is required.");
    return requireAccount(context.company(), context.branch(), context.currency(),
        context.documentGroup(), context.documentType(), role);
  }

  @Override
  public AccountingSetupResolution requireAccount(
      Branch branch, Currency currency, String group,
      String type, AccountingRoleDefinition role) {
    return requireAccount(activeOrganizationService.requireActiveCompany(),
        branch, currency, group, type, role);
  }

  protected List<AccountingSetupEntry> findCandidates(
      Company company, String group, AccountingRoleDefinition role) {
    return repository.all().filter("self.company = :company AND self.documentGroup = :group "
        + "AND self.accountingRoleDefinition = :role AND self.active = true "
        + "AND self.archived = false")
        .bind("company", company).bind("group", group).bind("role", role).fetch();
  }
  private void validateInput(
      Company company, Branch branch, Currency currency, String group,
      String type, AccountingRoleDefinition role) {
    if (company == null || Boolean.TRUE.equals(company.getArchived())
        || !Boolean.TRUE.equals(company.getActive())) throw error("Company must be active.");
    accessService.requireCompanyAccess(AuthUtils.getUser(), company);
    if (branch != null) {
      if (!same(branch.getCompany(), company)) throw error("Branch belongs to another company.");
      if (Boolean.TRUE.equals(branch.getArchived()) || !Boolean.TRUE.equals(branch.getActive()))
        throw error("Branch must be active.");
      accessService.requireBranchAccess(AuthUtils.getUser(), branch);
    }
    if (currency != null && Boolean.TRUE.equals(currency.getArchived()))
      throw error("Currency is archived.");
    roleService.requireUsable(role);
    validationService.validateRoleContext(group, type, role);
  }
  private boolean matches(
      AccountingSetupEntry entry, Branch branch, Currency currency, String type) {
    return (entry.getDocumentType() == null || Objects.equals(entry.getDocumentType(), type))
        && (entry.getBranch() == null || same(entry.getBranch(), branch))
        && (entry.getCurrency() == null || same(entry.getCurrency(), currency));
  }
  private int specificity(AccountingSetupEntry entry) {
    return (entry.getDocumentType() == null ? 0 : 4)
        + (entry.getBranch() == null ? 0 : 2)
        + (entry.getCurrency() == null ? 0 : 1);
  }
  private boolean same(com.axelor.db.Model left, com.axelor.db.Model right) {
    return left == right || left != null && right != null && left.getId() != null
        && Objects.equals(left.getId(), right.getId());
  }
  private IllegalArgumentException error(String message) {
    return new IllegalArgumentException(I18n.get(message));
  }
}
