package com.odc.accounting.service;

import com.odc.accounting.db.AccountingRoleDefinition;
import com.odc.organization.db.Branch;
import com.odc.organization.db.Company;
import com.odc.reference.db.Currency;
import java.util.Optional;

public interface AccountingSetupResolver {
  Optional<AccountingSetupResolution> findAccount(
      Company company, Branch branch, Currency currency, String documentGroup,
      String documentType, AccountingRoleDefinition roleDefinition);
  AccountingSetupResolution requireAccount(
      Company company, Branch branch, Currency currency, String documentGroup,
      String documentType, AccountingRoleDefinition roleDefinition);
  AccountingSetupResolution requireAccount(
      Branch branch, Currency currency, String documentGroup,
      String documentType, AccountingRoleDefinition roleDefinition);
}
