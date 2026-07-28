package com.odc.sales.service;
import com.odc.sales.db.SalesInvoice;
public interface SalesInvoiceConfirmationService {SalesInvoiceConfirmationResult confirm(SalesInvoice invoice);void validateForConfirmation(SalesInvoice invoice);}
