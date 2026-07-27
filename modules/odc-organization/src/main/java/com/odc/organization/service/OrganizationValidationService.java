package com.odc.organization.service;

import com.odc.reference.db.City;
import com.odc.reference.db.Country;
import com.odc.reference.db.Currency;

public interface OrganizationValidationService {

  String normalizeRequiredCode(String code, String field);

  String normalizeRequiredName(String name, String field);

  String normalizeTimezone(String timezone);

  String normalizeLocale(String locale);

  void requireActive(Country country);

  void requireActive(Currency currency);

  void requireActive(City city);
}
