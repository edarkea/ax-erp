package com.odc.reference.module;

import com.axelor.app.AxelorModule;
import com.odc.reference.service.CurrencyService;
import com.odc.reference.service.CurrencyServiceImpl;

public class OdcReferenceDataModule extends AxelorModule {

  @Override
  protected void configure() {
    bind(CurrencyService.class).to(CurrencyServiceImpl.class);
  }
}
