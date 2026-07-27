package com.odc.reference.module;

import com.axelor.app.AxelorModule;
import com.odc.reference.observer.CurrencySaveObserver;
import com.odc.reference.observer.GeographySaveObserver;
import com.odc.reference.service.CurrencyService;
import com.odc.reference.service.CurrencyServiceImpl;
import com.odc.reference.service.GeographyService;
import com.odc.reference.service.GeographyServiceImpl;

public class OdcReferenceDataModule extends AxelorModule {

  @Override
  protected void configure() {
    bind(CurrencyService.class).to(CurrencyServiceImpl.class);
    bind(GeographyService.class).to(GeographyServiceImpl.class);
    bind(CurrencySaveObserver.class);
    bind(GeographySaveObserver.class);
  }
}
