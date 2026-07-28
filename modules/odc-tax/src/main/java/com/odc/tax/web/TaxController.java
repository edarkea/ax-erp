package com.odc.tax.web;

import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.odc.tax.db.TaxCategory;
import com.odc.tax.db.TaxRate;
import com.odc.tax.service.TaxCategoryService;
import com.odc.tax.service.TaxRateService;

public class TaxController {

  public void validateCategory(ActionRequest request, ActionResponse response) {
    TaxCategory category = request.getContext().asType(TaxCategory.class);
    Beans.get(TaxCategoryService.class).validate(category);
    response.setValue("code", category.getCode());
    response.setValue("name", category.getName());
    response.setValue("archived", category.getArchived());
  }

  public void validateRate(ActionRequest request, ActionResponse response) {
    TaxRate rate = request.getContext().asType(TaxRate.class);
    Beans.get(TaxRateService.class).validate(rate);
    response.setValue("archived", rate.getArchived());
  }

  public void closeRate(ActionRequest request, ActionResponse response) {
    TaxRate rate = request.getContext().asType(TaxRate.class);
    TaxRate closed = Beans.get(TaxRateService.class).closeRate(rate, rate.getValidUntil());
    response.setValue("validUntil", closed.getValidUntil());
    response.setReload(true);
  }
}
