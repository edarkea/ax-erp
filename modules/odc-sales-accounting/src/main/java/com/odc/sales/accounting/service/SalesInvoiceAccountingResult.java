package com.odc.sales.accounting.service;

import com.odc.accounting.db.JournalEntry;
import com.odc.sales.db.SalesInvoice;

public record SalesInvoiceAccountingResult(
    SalesInvoice invoice, JournalEntry journalEntry, boolean alreadyPosted) {}
