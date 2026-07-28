package com.odc.accounting.service;

import com.odc.accounting.db.AccountingSetupEntry;

public interface AccountingSetupEntryService {
  AccountingSetupEntry save(AccountingSetupEntry entry);
  void validate(AccountingSetupEntry entry);
  void archive(AccountingSetupEntry entry);
  AccountingSetupEntry restore(AccountingSetupEntry entry);
  void requireUsable(AccountingSetupEntry entry);
}
