package com.odc.pricing.observer;

import static com.odc.common.rpc.RequestEntityUtils.process;

import com.axelor.event.Observes;
import com.axelor.events.PreRequest;
import com.axelor.events.RequestEvent;
import com.axelor.events.qualifiers.EntityType;
import com.odc.pricing.db.PriceList;
import com.odc.pricing.service.PriceListService;
import jakarta.inject.Inject;
import jakarta.inject.Named;

public class PriceListSaveObserver {

  private final PriceListService service;

  @Inject
  public PriceListSaveObserver(PriceListService service) {
    this.service = service;
  }

  public void onSave(
      @Observes @Named(RequestEvent.SAVE) @EntityType(PriceList.class) PreRequest event) {
    process(event, PriceList.class, service::validate);
  }
}
