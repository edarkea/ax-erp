package com.odc.sales.accounting.service;

import com.odc.accounting.db.JournalEntry;
import com.odc.accounting.db.JournalLine;
import com.odc.accounting.service.AccountingSetupResolution;
import com.odc.sales.db.SalesInvoice;
import java.math.BigDecimal;
import java.util.List;

public record SalesInvoicePostingPlan(
    SalesInvoice invoice,
    SalesInvoicePostingContext context,
    JournalEntry journalEntry,
    List<JournalLine> journalLines,
    AccountingSetupResolution accountReceivableResolution,
    AccountingSetupResolution salesRevenueResolution,
    AccountingSetupResolution outputTaxResolution,
    BigDecimal totalDebit,
    BigDecimal totalCredit,
    int lineCount) {}
