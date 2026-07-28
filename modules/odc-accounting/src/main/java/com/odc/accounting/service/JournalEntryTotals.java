package com.odc.accounting.service;

import com.odc.reference.db.Currency;
import java.math.BigDecimal;

public record JournalEntryTotals(
    BigDecimal totalDebit, BigDecimal totalCredit, BigDecimal difference,
    int lineCount, boolean balanced, boolean hasPositiveTotal,
    Currency currency, int currencyScale) {}
