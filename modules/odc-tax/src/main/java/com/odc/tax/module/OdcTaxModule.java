package com.odc.tax.module;

import com.axelor.app.AxelorModule;
import com.odc.tax.observer.TaxSaveObserver;
import com.odc.tax.service.TaxCalculationService;
import com.odc.tax.service.TaxCalculationServiceImpl;
import com.odc.tax.service.TaxCategoryService;
import com.odc.tax.service.TaxCategoryServiceImpl;
import com.odc.tax.service.TaxRateService;
import com.odc.tax.service.TaxRateServiceImpl;

public class OdcTaxModule extends AxelorModule {

  @Override
  protected void configure() {
    bind(TaxCategoryService.class).to(TaxCategoryServiceImpl.class);
    bind(TaxRateService.class).to(TaxRateServiceImpl.class);
    bind(TaxCalculationService.class).to(TaxCalculationServiceImpl.class);
    bind(TaxSaveObserver.class);
  }
}
