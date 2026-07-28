package com.odc.sales.accounting.service;

import com.odc.accounting.db.JournalEntry;
import com.odc.accounting.service.JournalReversalResult;
import com.odc.sales.db.SalesInvoice;
import java.time.LocalDate;
import java.util.Optional;

public interface SalesInvoiceAccountingService {
  SalesInvoiceAccountingResult postInvoice(SalesInvoice invoice);
  Optional<JournalEntry> findPosting(SalesInvoice invoice);
  JournalEntry requirePosting(SalesInvoice invoice);
  boolean isPosted(SalesInvoice invoice);
  JournalReversalResult reverseInvoicePosting(
      SalesInvoice invoice, LocalDate reversalDate, String reason);
  SalesInvoice cancelWithReversal(
      SalesInvoice invoice, LocalDate reversalDate, String reason);
}
