package com.odc.sales.accounting.service;

import com.odc.sales.db.SalesInvoice;

public interface SalesInvoicePostingMapper {
  SalesInvoicePostingPlan map(SalesInvoice invoice);
}
