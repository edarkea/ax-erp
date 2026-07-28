package com.odc.sales.service;
import com.odc.sales.db.*;
public interface SalesInvoiceTaxService { SalesInvoiceLine refreshTax(SalesInvoiceLine line); void refreshTaxes(SalesInvoice invoice); }
