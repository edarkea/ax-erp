package com.odc.accounting.service;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import com.odc.accounting.db.AccountingRoleDefinition;
import com.odc.accounting.db.repo.AccountingRoleDefinitionRepository;
import java.util.List;

public class AccountingRoleDefinitionSeedServiceImpl
    implements AccountingRoleDefinitionSeedService {
  private final AccountingRoleDefinitionRepository repository;
  private final AccountingRoleDefinitionService service;

  @Inject
  public AccountingRoleDefinitionSeedServiceImpl(
      AccountingRoleDefinitionRepository repository,
      AccountingRoleDefinitionService service) {
    this.repository = repository;
    this.service = service;
  }

  @Override @Transactional
  public void seed() {
    definitions().forEach(
        seed -> {
          AccountingRoleDefinition current = findByCode(seed.getCode());
          if (current == null) save(seed);
        });
  }

  protected AccountingRoleDefinition findByCode(String code) {
    return repository.all().filter("self.code = :code").bind("code", code).fetchOne();
  }

  protected AccountingRoleDefinition save(AccountingRoleDefinition value) {
    return service.save(value);
  }

  protected List<AccountingRoleDefinition> definitions() {
    return List.of(
        definition("ACCOUNT_RECEIVABLE", "Accounts receivable", "SALES", "DEBIT", true, true, false),
        definition("SALES_REVENUE", "Sales revenue", "SALES", "CREDIT", false, false, false),
        definition("OUTPUT_TAX", "Output tax", "SALES", "CREDIT", false, false, false),
        definition("SALES_DISCOUNT", "Sales discount", "SALES", "DEBIT", false, false, false),
        definition("CASH", "Cash", "TREASURY", "DEBIT", false, false, true));
  }

  private AccountingRoleDefinition definition(
      String code, String name, String group, String side,
      boolean party, boolean dueDate, boolean manual) {
    AccountingRoleDefinition value = new AccountingRoleDefinition();
    value.setCode(code); value.setName(name); value.setDocumentGroup(group);
    value.setSideHint(side); value.setRequiresParty(party);
    value.setRequiresDueDate(dueDate); value.setAllowManualSelection(manual);
    value.setSystemDefined(true); value.setActive(true); value.setArchived(false);
    return value;
  }
}
