package com.odc.sales.service;
import com.odc.sales.db.SalesInvoice;
public interface SalesInvoiceService {
 SalesInvoice save(SalesInvoice invoice); void validateDraft(SalesInvoice invoice); void validateCommon(SalesInvoice invoice);
 void archive(SalesInvoice invoice); SalesInvoice restore(SalesInvoice invoice);
 void requireEditable(SalesInvoice invoice); void requireUsable(SalesInvoice invoice);
}
