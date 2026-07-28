package com.odc.catalog.module;
import com.axelor.app.AxelorModule;
import com.odc.catalog.service.*;
import com.odc.catalog.observer.CatalogSaveObserver;
public class OdcCatalogModule extends AxelorModule {
  @Override protected void configure(){
    bind(UnitOfMeasureService.class).to(UnitOfMeasureServiceImpl.class);
    bind(ItemCategoryService.class).to(ItemCategoryServiceImpl.class);
    bind(CatalogValidationService.class).to(CatalogValidationServiceImpl.class);
    bind(ItemService.class).to(ItemServiceImpl.class);
    bind(CatalogSaveObserver.class);
  }
}
