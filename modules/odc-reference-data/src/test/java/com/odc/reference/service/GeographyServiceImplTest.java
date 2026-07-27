package com.odc.reference.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odc.reference.db.City;
import com.odc.reference.db.Country;
import com.odc.reference.db.Currency;
import com.odc.reference.db.State;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GeographyServiceImplTest {

  private TestGeographyService service;

  @BeforeEach
  void setUp() {
    service = new TestGeographyService();
  }

  @Test
  void shouldValidateAndNormalizeValidHierarchy() {
    Country country = country(" ec ");
    State state = state(" p ", country);
    City city = city(" qui ", state);

    service.validateCountry(country);
    service.validateState(state);
    service.validateCity(city);

    assertEquals("EC", country.getCode());
    assertEquals("P", state.getCode());
    assertEquals("QUI", city.getCode());
    assertFalse(country.getArchived());
    assertFalse(state.getArchived());
    assertFalse(city.getArchived());
  }

  @Test
  void shouldInitializeAndValidateStatesSavedInsideCountry() {
    Country country = country(" ec ");
    State state = state(" 010102 ", null);
    country.setStates(new ArrayList<>(List.of(state)));

    service.validateCountry(country);

    assertNull(state.getCountry());
    assertEquals("010102", state.getCode());
    assertFalse(state.getArchived());
  }

  @Test
  void shouldInitializeAndValidateCitiesSavedInsideState() {
    State state = state(" p ", country("EC"));
    City city = city(" gye ", null);
    state.setCities(new ArrayList<>(List.of(city)));

    service.validateState(state);

    assertNull(city.getState());
    assertEquals("GYE", city.getCode());
    assertFalse(city.getArchived());
  }

  @Test
  void shouldRejectRepeatedStateCodeInsideSameCountry() {
    Country country = country("EC");
    State duplicate = state("P", country);
    service.duplicateState = duplicate;

    assertThrows(
        IllegalArgumentException.class, () -> service.validateState(state(" p ", country)));
  }

  @Test
  void shouldRejectRepeatedCountryCode() {
    service.duplicateCountry = country("EC");

    assertThrows(
        IllegalArgumentException.class, () -> service.validateCountry(country(" ec ")));
  }

  @Test
  void shouldAllowSameStateCodeInDifferentCountries() {
    Country ecuador = country("EC");
    Country argentina = country("AR");
    State existing = state("P", ecuador);
    service.duplicateState = existing;
    service.duplicateStateCountry = ecuador;

    service.validateState(state("P", argentina));

    assertEquals("P", service.lastValidatedStateCode);
  }

  @Test
  void shouldAllowSameCityCodeInDifferentStates() {
    Country country = country("EC");
    State pichincha = state("P", country);
    State guayas = state("G", country);
    service.duplicateCity = city("CENTRO", pichincha);
    service.duplicateCityState = pichincha;

    City city = city("CENTRO", guayas);
    service.validateCity(city);

    assertNull(service.findOtherActiveCity(guayas, "OTHER", null));
  }

  @Test
  void shouldRejectRepeatedCityCodeInsideSameState() {
    State state = state("P", country("EC"));
    service.duplicateCity = city("CENTRO", state);

    assertThrows(
        IllegalArgumentException.class, () -> service.validateCity(city(" centro ", state)));
  }

  @Test
  void shouldRejectCityInArchivedState() {
    State state = state("P", country("EC"));
    state.setArchived(true);

    assertThrows(
        IllegalArgumentException.class, () -> service.validateCity(city("QUI", state)));
  }

  @Test
  void shouldRejectCountryWithArchivedDefaultCurrency() {
    Currency currency = new Currency();
    currency.setCode("USD");
    currency.setArchived(true);
    Country country = country("EC");
    country.setDefaultCurrency(currency);

    assertThrows(IllegalArgumentException.class, () -> service.validateCountry(country));
  }

  @Test
  void shouldRejectArchivingCountryWithActiveStates() {
    Country country = country("EC");
    country.setId(1L);
    country.setArchived(true);
    service.activeStates = true;

    assertThrows(IllegalArgumentException.class, () -> service.validateCountry(country));
  }

  @Test
  void shouldRejectArchivingStateWithActiveCities() {
    State state = state("P", country("EC"));
    state.setId(2L);
    state.setArchived(true);
    service.activeCities = true;

    assertThrows(IllegalArgumentException.class, () -> service.validateState(state));
  }

  private Country country(String code) {
    Country country = new Country();
    country.setCode(code);
    country.setName("Country " + code.trim());
    return country;
  }

  private State state(String code, Country country) {
    State state = new State();
    state.setCode(code);
    state.setName("State " + code.trim());
    state.setCountry(country);
    return state;
  }

  private City city(String code, State state) {
    City city = new City();
    city.setCode(code);
    city.setName("City " + code.trim());
    city.setState(state);
    return city;
  }

  private static class TestGeographyService extends GeographyServiceImpl {

    private State duplicateState;
    private Country duplicateCountry;
    private Country duplicateStateCountry;
    private City duplicateCity;
    private State duplicateCityState;
    private boolean activeStates;
    private boolean activeCities;
    private String lastValidatedStateCode;

    private TestGeographyService() {
      super(null, null, null);
    }

    @Override
    protected Country findOtherActiveCountry(String code, Long excludedId) {
      return duplicateCountry;
    }

    @Override
    protected State findOtherActiveState(Country country, String code, Long excludedId) {
      lastValidatedStateCode = code;
      if (duplicateStateCountry == null || duplicateStateCountry == country) {
        return duplicateState;
      }
      return null;
    }

    @Override
    protected City findOtherActiveCity(State state, String code, Long excludedId) {
      if (duplicateCityState == null || duplicateCityState == state) {
        return duplicateCity;
      }
      return null;
    }

    @Override
    protected boolean hasActiveStates(Country country) {
      return activeStates;
    }

    @Override
    protected boolean hasActiveCities(State state) {
      return activeCities;
    }
  }
}
