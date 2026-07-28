package com.odc.sales.accounting.service;

import com.odc.sales.db.SalesInvoice;

public interface SalesInvoicePostingValidator {
  void validateForPosting(SalesInvoice invoice);
  boolean isReadyForPosting(SalesInvoice invoice);
  SalesInvoicePostingContext validateAndResolve(SalesInvoice invoice);
}
