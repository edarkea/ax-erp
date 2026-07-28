package com.odc.accounting.service;

import com.odc.accounting.db.AccountingRoleDefinition;
import com.odc.accounting.db.AccountingSetupEntry;
import com.odc.accounting.db.ChartAccount;
import com.odc.organization.db.Branch;
import com.odc.organization.db.Company;
import com.odc.reference.db.Currency;

public record AccountingSetupResolution(
    AccountingSetupEntry setupEntry,
    ChartAccount account,
    AccountingRoleDefinition roleDefinition,
    Company company,
    Branch branch,
    Currency currency,
    int specificityScore) {}
