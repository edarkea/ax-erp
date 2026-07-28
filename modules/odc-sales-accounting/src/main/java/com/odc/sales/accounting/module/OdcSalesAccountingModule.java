package com.odc.sales.accounting.module;

import com.axelor.app.AxelorModule;
import com.odc.sales.accounting.service.*;

public class OdcSalesAccountingModule extends AxelorModule {
  @Override
  protected void configure() {
    bind(SalesInvoicePostingValidator.class).to(SalesInvoicePostingValidatorImpl.class);
    bind(SalesInvoicePostingMapper.class).to(SalesInvoicePostingMapperImpl.class);
    bind(SalesInvoiceAccountingService.class).to(SalesInvoiceAccountingServiceImpl.class);
  }
}
