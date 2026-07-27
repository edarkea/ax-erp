package com.odc.reference.web;

import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.odc.reference.db.City;
import com.odc.reference.db.Country;
import com.odc.reference.db.State;
import com.odc.reference.service.GeographyService;

public class GeographyController {

  public void validateCountry(ActionRequest request, ActionResponse response) {
    Country country = request.getContext().asType(Country.class);
    Beans.get(GeographyService.class).validateCountry(country);
    response.setValue("code", country.getCode());
    response.setValue("archived", country.getArchived());
  }

  public void validateState(ActionRequest request, ActionResponse response) {
    State state = request.getContext().asType(State.class);
    Beans.get(GeographyService.class).validateState(state);
    response.setValue("code", state.getCode());
    response.setValue("archived", state.getArchived());
  }

  public void validateCity(ActionRequest request, ActionResponse response) {
    City city = request.getContext().asType(City.class);
    Beans.get(GeographyService.class).validateCity(city);
    response.setValue("code", city.getCode());
    response.setValue("archived", city.getArchived());
  }
}
