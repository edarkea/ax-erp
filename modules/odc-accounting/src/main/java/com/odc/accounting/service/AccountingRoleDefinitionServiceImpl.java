package com.odc.accounting.service;

import com.axelor.i18n.I18n;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import com.odc.accounting.db.AccountingRoleDefinition;
import com.odc.accounting.db.repo.AccountingRoleDefinitionRepository;
import com.odc.accounting.db.repo.AccountingSetupEntryRepository;
import java.util.Objects;

public class AccountingRoleDefinitionServiceImpl implements AccountingRoleDefinitionService {
  private final AccountingRoleDefinitionRepository repository;
  private final AccountingSetupEntryRepository setupRepository;
  private final AccountingConfigurationValidationService validationService;

  @Inject
  public AccountingRoleDefinitionServiceImpl(
      AccountingRoleDefinitionRepository repository,
      AccountingSetupEntryRepository setupRepository,
      AccountingConfigurationValidationService validationService) {
    this.repository = repository;
    this.setupRepository = setupRepository;
    this.validationService = validationService;
  }

  @Override @Transactional
  public AccountingRoleDefinition save(AccountingRoleDefinition value) {
    validate(value);
    return persist(value);
  }

  @Override
  public void validate(AccountingRoleDefinition value) {
    if (value == null) throw error("Accounting role definition is required.");
    defaults(value);
    value.setCode(required(value.getCode(), "Accounting role code is required.").toUpperCase());
    value.setName(required(value.getName(), "Accounting role name is required."));
    if (value.getSideHint() == null
        || !java.util.Set.of("DEBIT", "CREDIT", "EITHER").contains(value.getSideHint()))
      throw error("Accounting role side is required.");
    validationService.validateDocumentContext(value.getDocumentGroup(), value.getDocumentType());
    if (!Boolean.TRUE.equals(value.getArchived()) && findDuplicate(value) != null)
      throw error("Accounting role already exists.");
    AccountingRoleDefinition persisted = findPersisted(value.getId());
    if (persisted != null && Boolean.TRUE.equals(persisted.getSystemDefined())
        && !Objects.equals(persisted.getCode(), value.getCode()))
      throw error("Cannot modify a system accounting role.");
    if (persisted != null && hasActiveSetupEntries(value)
        && (!Objects.equals(persisted.getDocumentGroup(), value.getDocumentGroup())
            || !Objects.equals(persisted.getDocumentType(), value.getDocumentType())
            || !Objects.equals(persisted.getSideHint(), value.getSideHint())))
      throw error("Accounting role structure cannot change while it has active configurations.");
  }

  @Override @Transactional
  public void archive(AccountingRoleDefinition value) {
    requirePersisted(value);
    if (Boolean.TRUE.equals(value.getSystemDefined()))
      throw error("Cannot archive a system accounting role.");
    if (hasActiveSetupEntries(value))
      throw error("Cannot archive an accounting role used by active configurations.");
    value.setActive(false); value.setArchived(true); persist(value);
  }

  @Override @Transactional
  public AccountingRoleDefinition restore(AccountingRoleDefinition value) {
    requirePersisted(value);
    value.setArchived(false); value.setActive(true); validate(value); return persist(value);
  }

  @Override
  public void requireUsable(AccountingRoleDefinition value) {
    if (value == null || Boolean.TRUE.equals(value.getArchived())
        || !Boolean.TRUE.equals(value.getActive()))
      throw error("Accounting role must be active and not archived.");
  }

  protected AccountingRoleDefinition findDuplicate(AccountingRoleDefinition value) {
    String filter = "self.code = :code AND self.archived = false";
    var query = repository.all().filter(filter).bind("code", value.getCode());
    if (value.getId() != null) query = repository.all().filter(filter + " AND self.id != :id")
        .bind("code", value.getCode()).bind("id", value.getId());
    return query.fetchOne();
  }
  protected boolean hasActiveSetupEntries(AccountingRoleDefinition value) {
    if (value.getId() == null) return false;
    return setupRepository.all().filter("self.accountingRoleDefinition = :role "
        + "AND self.active = true AND self.archived = false").bind("role", value).count() > 0;
  }
  protected AccountingRoleDefinition findPersisted(Long id) {
    return id == null ? null : repository.find(id);
  }
  protected AccountingRoleDefinition persist(AccountingRoleDefinition value) {
    return repository.save(value);
  }
  private void defaults(AccountingRoleDefinition value) {
    if (value.getArchived() == null) value.setArchived(false);
    if (value.getActive() == null) value.setActive(true);
    if (value.getRequiresParty() == null) value.setRequiresParty(false);
    if (value.getRequiresDueDate() == null) value.setRequiresDueDate(false);
    if (value.getAllowManualSelection() == null) value.setAllowManualSelection(false);
    if (value.getSystemDefined() == null) value.setSystemDefined(false);
  }
  private String required(String value, String message) {
    if (value == null || value.trim().isEmpty()) throw error(message);
    return value.trim();
  }
  private void requirePersisted(AccountingRoleDefinition value) {
    if (value == null || value.getId() == null)
      throw error("Accounting role definition is required.");
  }
  private IllegalArgumentException error(String message) {
    return new IllegalArgumentException(I18n.get(message));
  }
}
