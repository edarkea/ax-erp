package com.odc.sales.accounting.service;

import com.odc.accounting.db.AccountingPeriod;
import com.odc.accounting.service.AccountingSetupResolution;

public record SalesInvoicePostingContext(
    AccountingPeriod period,
    AccountingSetupResolution receivable,
    AccountingSetupResolution revenue,
    AccountingSetupResolution outputTax) {}
