package com.odc.accounting.service;

import com.odc.accounting.db.AccountingRoleDefinition;
import com.odc.accounting.db.ChartAccount;

public interface AccountingConfigurationValidationService {
  void validateDocumentContext(String documentGroup, String documentType);
  void validateRoleContext(
      String documentGroup, String documentType, AccountingRoleDefinition definition);
  void validateAccountCompatibility(
      AccountingRoleDefinition definition, ChartAccount account);
}
