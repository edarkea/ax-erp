package com.odc.accounting.service;

import com.axelor.i18n.I18n;
import com.odc.accounting.db.AccountingRoleDefinition;
import com.odc.accounting.db.ChartAccount;
import java.util.Map;
import java.util.Set;

public class AccountingConfigurationValidationServiceImpl
    implements AccountingConfigurationValidationService {
  private static final Set<String> GROUPS = Set.of("SALES", "PURCHASES", "TREASURY", "GENERAL");
  private static final Set<String> SALES =
      Set.of("SALES_INVOICE", "CREDIT_NOTE", "DEBIT_NOTE", "WITHHOLDING", "SHIPPING_GUIDE");
  private static final Map<String, String> STRICT_TYPE =
      Map.of(
          "ACCOUNT_RECEIVABLE", "ASSET",
          "SALES_REVENUE", "INCOME",
          "OUTPUT_TAX", "LIABILITY",
          "SALES_DISCOUNT", "EXPENSE");

  @Override
  public void validateDocumentContext(String group, String type) {
    if (group == null || !GROUPS.contains(group)) throw error("Document group is required.");
    if (type == null || type.isBlank()) return;
    boolean valid =
        ("SALES".equals(group) && SALES.contains(type))
            || ("GENERAL".equals(group) && "GENERAL_ENTRY".equals(type))
            || (("PURCHASES".equals(group) || "TREASURY".equals(group))
                && !"GENERAL_ENTRY".equals(type));
    if (!valid) throw error("Document type is not compatible with document group.");
  }

  @Override
  public void validateRoleContext(
      String group, String type, AccountingRoleDefinition definition) {
    validateDocumentContext(group, type);
    if (definition == null) throw error("Accounting role definition is required.");
    if (!group.equals(definition.getDocumentGroup()))
      throw error("Accounting role does not belong to the selected document group.");
    if (definition.getDocumentType() != null
        && !definition.getDocumentType().equals(type))
      throw error("Accounting role does not support the selected document type.");
  }

  @Override
  public void validateAccountCompatibility(
      AccountingRoleDefinition definition, ChartAccount account) {
    if (definition == null || account == null) return;
    if ("DEBIT".equals(definition.getSideHint()) && !"DEBIT".equals(account.getNormalBalance())
        || "CREDIT".equals(definition.getSideHint())
            && !"CREDIT".equals(account.getNormalBalance()))
      throw error("Account normal balance is incompatible with the accounting role.");
    String expected = STRICT_TYPE.get(definition.getCode());
    if (expected != null && !expected.equals(account.getAccountType()))
      throw error("Account type is incompatible with the system accounting role.");
  }

  private IllegalArgumentException error(String message) {
    return new IllegalArgumentException(I18n.get(message));
  }
}
