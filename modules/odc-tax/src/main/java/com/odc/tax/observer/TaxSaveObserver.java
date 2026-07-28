package com.odc.tax.observer;

import static com.odc.common.rpc.RequestEntityUtils.process;

import com.axelor.event.Observes;
import com.axelor.events.PreRequest;
import com.axelor.events.RequestEvent;
import com.axelor.events.qualifiers.EntityType;
import com.odc.tax.db.TaxCategory;
import com.odc.tax.db.TaxRate;
import com.odc.tax.service.TaxCategoryService;
import com.odc.tax.service.TaxRateService;
import jakarta.inject.Inject;
import jakarta.inject.Named;

public class TaxSaveObserver {

  private final TaxCategoryService categoryService;
  private final TaxRateService rateService;

  @Inject
  public TaxSaveObserver(TaxCategoryService categoryService, TaxRateService rateService) {
    this.categoryService = categoryService;
    this.rateService = rateService;
  }

  public void onCategorySave(
      @Observes @Named(RequestEvent.SAVE) @EntityType(TaxCategory.class) PreRequest event) {
    process(event, TaxCategory.class, categoryService::validate);
  }

  public void onRateSave(
      @Observes @Named(RequestEvent.SAVE) @EntityType(TaxRate.class) PreRequest event) {
    process(event, TaxRate.class, rateService::validate);
  }
}
