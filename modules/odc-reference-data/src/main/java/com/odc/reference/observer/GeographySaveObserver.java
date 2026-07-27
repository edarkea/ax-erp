package com.odc.reference.observer;

import static com.odc.common.rpc.RequestEntityUtils.process;

import com.axelor.event.Observes;
import com.axelor.events.PreRequest;
import com.axelor.events.RequestEvent;
import com.axelor.events.qualifiers.EntityType;
import com.odc.reference.db.City;
import com.odc.reference.db.Country;
import com.odc.reference.db.State;
import com.odc.reference.service.GeographyService;
import jakarta.inject.Inject;
import jakarta.inject.Named;

public class GeographySaveObserver {

  private final GeographyService geographyService;

  @Inject
  public GeographySaveObserver(GeographyService geographyService) {
    this.geographyService = geographyService;
  }

  public void onCountrySave(
      @Observes @Named(RequestEvent.SAVE) @EntityType(Country.class) PreRequest event) {
    process(event, Country.class, geographyService::validateCountry);
  }

  public void onStateSave(
      @Observes @Named(RequestEvent.SAVE) @EntityType(State.class) PreRequest event) {
    process(event, State.class, geographyService::validateState);
  }

  public void onCitySave(
      @Observes @Named(RequestEvent.SAVE) @EntityType(City.class) PreRequest event) {
    process(event, City.class, geographyService::validateCity);
  }
}
