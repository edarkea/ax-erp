package com.odc.reference.service;

import com.odc.reference.db.City;
import com.odc.reference.db.Country;
import com.odc.reference.db.State;

public interface GeographyService {

  void validateCountry(Country country);

  void validateState(State state);

  void validateCity(City city);
}
