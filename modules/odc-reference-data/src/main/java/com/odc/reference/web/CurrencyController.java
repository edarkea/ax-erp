package com.odc.reference.web;

import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.odc.reference.db.Currency;
import com.odc.reference.service.CurrencyService;

public class CurrencyController {

  public void validate(ActionRequest request, ActionResponse response) {
    Currency currency = request.getContext().asType(Currency.class);
    Beans.get(CurrencyService.class).validate(currency);
    response.setValue("code", currency.getCode());
    response.setValue("decimalPlaces", currency.getDecimalPlaces());
  }

  public void archive(ActionRequest request, ActionResponse response) {
    Currency currency = request.getContext().asType(Currency.class);
    Beans.get(CurrencyService.class).archive(currency);
    response.setReload(true);
  }
}
