package com.odc.accounting.service;

import com.odc.organization.db.Branch;
import com.odc.organization.db.Company;
import com.odc.party.db.Party;
import com.odc.reference.db.Currency;
import java.time.LocalDate;

public record AccountingPostingContext(
    Company company, Branch branch, Currency currency, String documentGroup,
    String documentType, LocalDate accountingDate, Party party,
    String sourceModule, String sourceModel, Long sourceRecordId) {}
