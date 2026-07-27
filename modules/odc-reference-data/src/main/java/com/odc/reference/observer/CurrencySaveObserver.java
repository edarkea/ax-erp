package com.odc.reference.observer;

import static com.odc.common.rpc.RequestEntityUtils.process;

import com.axelor.event.Observes;
import com.axelor.events.PreRequest;
import com.axelor.events.RequestEvent;
import com.axelor.events.qualifiers.EntityType;
import com.odc.reference.db.Currency;
import com.odc.reference.service.CurrencyService;
import jakarta.inject.Inject;
import jakarta.inject.Named;

public class CurrencySaveObserver {

  private final CurrencyService currencyService;

  @Inject
  public CurrencySaveObserver(CurrencyService currencyService) {
    this.currencyService = currencyService;
  }

  public void onCurrencySave(
      @Observes @Named(RequestEvent.SAVE) @EntityType(Currency.class) PreRequest event) {
    process(event, Currency.class, currencyService::validate);
  }
}
