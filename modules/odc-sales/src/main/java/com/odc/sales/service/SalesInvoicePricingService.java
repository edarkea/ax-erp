package com.odc.sales.service;
import com.odc.sales.db.*;
public interface SalesInvoicePricingService { SalesInvoiceLine refreshPrice(SalesInvoiceLine line); void refreshPrices(SalesInvoice invoice); }
