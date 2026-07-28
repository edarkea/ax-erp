package com.odc.sales.service;
import com.odc.sales.db.*;
public interface SalesInvoiceCalculationService {
  SalesInvoiceLineTotals calculateLine(SalesInvoiceLine line);
  SalesInvoiceTotals calculate(SalesInvoice invoice);
  SalesInvoiceTotals recalculate(SalesInvoice invoice);
}
