package com.odc.pricing.module;

import com.axelor.app.AxelorModule;
import com.odc.pricing.observer.PriceListSaveObserver;
import com.odc.pricing.observer.PriceListItemSaveObserver;
import com.odc.pricing.service.PriceListItemService;
import com.odc.pricing.service.PriceListItemServiceImpl;
import com.odc.pricing.service.PriceListService;
import com.odc.pricing.service.PriceListServiceImpl;
import com.odc.pricing.service.PriceResolverService;
import com.odc.pricing.service.PriceResolverServiceImpl;

public class OdcPricingModule extends AxelorModule {

  @Override
  protected void configure() {
    bind(PriceListService.class).to(PriceListServiceImpl.class);
    bind(PriceListItemService.class).to(PriceListItemServiceImpl.class);
    bind(PriceResolverService.class).to(PriceResolverServiceImpl.class);
    bind(PriceListSaveObserver.class);
    bind(PriceListItemSaveObserver.class);
  }
}
