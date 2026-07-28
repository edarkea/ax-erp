package com.odc.pricing.observer;

import static com.odc.common.rpc.RequestEntityUtils.process;

import com.axelor.event.Observes;
import com.axelor.events.PreRequest;
import com.axelor.events.RequestEvent;
import com.axelor.events.qualifiers.EntityType;
import com.odc.pricing.db.PriceListItem;
import com.odc.pricing.service.PriceListItemService;
import jakarta.inject.Inject;
import jakarta.inject.Named;

public class PriceListItemSaveObserver {

  private final PriceListItemService service;

  @Inject
  public PriceListItemSaveObserver(PriceListItemService service) {
    this.service = service;
  }

  public void onSave(
      @Observes @Named(RequestEvent.SAVE) @EntityType(PriceListItem.class) PreRequest event) {
    process(event, PriceListItem.class, service::validate);
  }
}
