package com.odc.accounting.service;

import com.odc.accounting.db.AccountingRoleDefinition;

public interface AccountingRoleDefinitionService {
  AccountingRoleDefinition save(AccountingRoleDefinition definition);
  void validate(AccountingRoleDefinition definition);
  void archive(AccountingRoleDefinition definition);
  AccountingRoleDefinition restore(AccountingRoleDefinition definition);
  void requireUsable(AccountingRoleDefinition definition);
  default AccountingRoleDefinition requireByCode(String code) {
    throw new UnsupportedOperationException("Accounting role lookup is not implemented.");
  }
}
