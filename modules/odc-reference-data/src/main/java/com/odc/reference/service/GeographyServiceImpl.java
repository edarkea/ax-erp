package com.odc.reference.service;

import com.axelor.i18n.I18n;
import com.google.inject.Inject;
import com.odc.reference.db.City;
import com.odc.reference.db.Country;
import com.odc.reference.db.Currency;
import com.odc.reference.db.State;
import com.odc.reference.db.repo.CityRepository;
import com.odc.reference.db.repo.CountryRepository;
import com.odc.reference.db.repo.StateRepository;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class GeographyServiceImpl implements GeographyService {

  private final CountryRepository countryRepository;
  private final StateRepository stateRepository;
  private final CityRepository cityRepository;

  @Inject
  public GeographyServiceImpl(
      CountryRepository countryRepository,
      StateRepository stateRepository,
      CityRepository cityRepository) {
    this.countryRepository = countryRepository;
    this.stateRepository = stateRepository;
    this.cityRepository = cityRepository;
  }

  @Override
  public void validateCountry(Country country) {
    if (country == null) {
      throw inconsistency("Country is required.");
    }
    initializeArchived(country);
    country.setCode(normalizeRequiredCode(country.getCode(), "Country code is required."));
    requireName(country.getName(), "Country name is required.");

    Currency defaultCurrency = country.getDefaultCurrency();
    if (defaultCurrency != null && Boolean.TRUE.equals(defaultCurrency.getArchived())) {
      throw inconsistency("Country default currency must be active.");
    }
    if (findOtherActiveCountry(country.getCode(), country.getId()) != null) {
      throw inconsistency("An active country with code {0} already exists.", country.getCode());
    }
    validateStates(country);
    if (Boolean.TRUE.equals(country.getArchived()) && hasActiveStates(country)) {
      throw inconsistency("Country cannot be archived while it has active states.");
    }
  }

  @Override
  public void validateState(State state) {
    validateState(state, null);
  }

  private void validateState(State state, Country owningCountry) {
    if (state == null) {
      throw inconsistency("State is required.");
    }
    initializeArchived(state);
    state.setCode(normalizeRequiredCode(state.getCode(), "State code is required."));
    requireName(state.getName(), "State name is required.");

    Country country = owningCountry != null ? owningCountry : state.getCountry();
    if (country == null) {
      throw inconsistency("State country is required.");
    }
    if (Boolean.TRUE.equals(country.getArchived())) {
      throw inconsistency("State country must be active.");
    }
    if (findOtherActiveState(country, state.getCode(), state.getId()) != null) {
      throw inconsistency(
          "An active state with code {0} already exists in country {1}.",
          state.getCode(), country.getCode());
    }
    validateCities(state, country);
    if (Boolean.TRUE.equals(state.getArchived()) && hasActiveCities(state)) {
      throw inconsistency("State cannot be archived while it has active cities.");
    }
  }

  @Override
  public void validateCity(City city) {
    validateCity(city, null, null);
  }

  private void validateCity(City city, State owningState, Country owningCountry) {
    if (city == null) {
      throw inconsistency("City is required.");
    }
    initializeArchived(city);
    city.setCode(normalizeRequiredCode(city.getCode(), "City code is required."));
    requireName(city.getName(), "City name is required.");

    State state = owningState != null ? owningState : city.getState();
    if (state == null) {
      throw inconsistency("City state is required.");
    }
    if (Boolean.TRUE.equals(state.getArchived())) {
      throw inconsistency("City state must be active.");
    }
    Country country = owningCountry != null ? owningCountry : state.getCountry();
    if (country == null || Boolean.TRUE.equals(country.getArchived())) {
      throw inconsistency("City state country must be active.");
    }
    if (findOtherActiveCity(state, city.getCode(), city.getId()) != null) {
      throw inconsistency(
          "An active city with code {0} already exists in state {1}.",
          city.getCode(), state.getCode());
    }
  }

  protected Country findOtherActiveCountry(String code, Long excludedId) {
    if (excludedId == null) {
      return countryRepository
          .all()
          .filter("self.code = :code AND self.archived = false")
          .bind("code", code)
          .fetchOne();
    }
    return countryRepository
        .all()
        .filter("self.code = :code AND self.archived = false AND self.id != :id")
        .bind("code", code)
        .bind("id", excludedId)
        .fetchOne();
  }

  protected State findOtherActiveState(Country country, String code, Long excludedId) {
    if (excludedId == null) {
      return stateRepository
          .all()
          .filter("self.country = :country AND self.code = :code AND self.archived = false")
          .bind("country", country)
          .bind("code", code)
          .fetchOne();
    }
    return stateRepository
        .all()
        .filter(
            "self.country = :country AND self.code = :code "
                + "AND self.archived = false AND self.id != :id")
        .bind("country", country)
        .bind("code", code)
        .bind("id", excludedId)
        .fetchOne();
  }

  protected City findOtherActiveCity(State state, String code, Long excludedId) {
    if (excludedId == null) {
      return cityRepository
          .all()
          .filter("self.state = :state AND self.code = :code AND self.archived = false")
          .bind("state", state)
          .bind("code", code)
          .fetchOne();
    }
    return cityRepository
        .all()
        .filter(
            "self.state = :state AND self.code = :code "
                + "AND self.archived = false AND self.id != :id")
        .bind("state", state)
        .bind("code", code)
        .bind("id", excludedId)
        .fetchOne();
  }

  protected boolean hasActiveStates(Country country) {
    return country.getId() != null
        && stateRepository
                .all()
                .filter("self.country = :country AND self.archived = false")
                .bind("country", country)
                .count()
            > 0;
  }

  protected boolean hasActiveCities(State state) {
    return state.getId() != null
        && cityRepository
                .all()
                .filter("self.state = :state AND self.archived = false")
                .bind("state", state)
                .count()
            > 0;
  }

  private void validateStates(Country country) {
    if (country.getStates() == null) {
      return;
    }

    Set<String> activeCodes = new HashSet<>();
    for (State state : country.getStates()) {
      validateState(state, country);
      if (!Boolean.TRUE.equals(state.getArchived()) && !activeCodes.add(state.getCode())) {
        throw inconsistency(
            "An active state with code {0} already exists in country {1}.",
            state.getCode(), country.getCode());
      }
    }
  }

  private void validateCities(State state, Country country) {
    if (state.getCities() == null) {
      return;
    }

    Set<String> activeCodes = new HashSet<>();
    for (City city : state.getCities()) {
      validateCity(city, state, country);
      if (!Boolean.TRUE.equals(city.getArchived()) && !activeCodes.add(city.getCode())) {
        throw inconsistency(
            "An active city with code {0} already exists in state {1}.",
            city.getCode(), state.getCode());
      }
    }
  }

  private String normalizeRequiredCode(String code, String requiredMessage) {
    String normalized = code == null ? null : code.trim().toUpperCase(Locale.ROOT);
    if (normalized == null || normalized.isEmpty()) {
      throw inconsistency(requiredMessage);
    }
    return normalized;
  }

  private void requireName(String name, String message) {
    if (name == null || name.trim().isEmpty()) {
      throw inconsistency(message);
    }
  }

  private void initializeArchived(Country country) {
    if (country.getArchived() == null) {
      country.setArchived(false);
    }
  }

  private void initializeArchived(State state) {
    if (state.getArchived() == null) {
      state.setArchived(false);
    }
  }

  private void initializeArchived(City city) {
    if (city.getArchived() == null) {
      city.setArchived(false);
    }
  }

  private IllegalArgumentException inconsistency(String message, Object... args) {
    String translated = I18n.get(message);
    for (int index = 0; index < args.length; index++) {
      translated = translated.replace("{" + index + "}", String.valueOf(args[index]));
    }
    return new IllegalArgumentException(translated);
  }
}
