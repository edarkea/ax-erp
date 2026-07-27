package com.odc.organization.service;

import com.axelor.i18n.I18n;
import com.odc.reference.db.City;
import com.odc.reference.db.Country;
import com.odc.reference.db.Currency;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.IllformedLocaleException;
import java.util.Locale;

public class OrganizationValidationServiceImpl implements OrganizationValidationService {

  @Override
  public String normalizeRequiredCode(String code, String field) {
    String normalized = code == null ? null : code.trim().toUpperCase(Locale.ROOT);
    if (normalized == null || normalized.isEmpty()) {
      throw inconsistency("{0} is required.", field);
    }
    return normalized;
  }

  @Override
  public String normalizeRequiredName(String name, String field) {
    String normalized = name == null ? null : name.trim();
    if (normalized == null || normalized.isEmpty()) {
      throw inconsistency("{0} is required.", field);
    }
    return normalized;
  }

  @Override
  public String normalizeTimezone(String timezone) {
    if (timezone == null || timezone.trim().isEmpty()) {
      return null;
    }
    String normalized = timezone.trim();
    try {
      return ZoneId.of(normalized).getId();
    } catch (DateTimeException exception) {
      throw inconsistency("Timezone {0} is invalid.", normalized);
    }
  }

  @Override
  public String normalizeLocale(String locale) {
    if (locale == null || locale.trim().isEmpty()) {
      return null;
    }
    String normalized = locale.trim().replace('_', '-');
    try {
      return new Locale.Builder().setLanguageTag(normalized).build().toLanguageTag();
    } catch (IllformedLocaleException exception) {
      throw inconsistency("Locale {0} is invalid.", normalized);
    }
  }

  @Override
  public void requireActive(Country country) {
    if (country != null && Boolean.TRUE.equals(country.getArchived())) {
      throw inconsistency("Country must be active.");
    }
  }

  @Override
  public void requireActive(Currency currency) {
    if (currency != null && Boolean.TRUE.equals(currency.getArchived())) {
      throw inconsistency("Default currency must be active.");
    }
  }

  @Override
  public void requireActive(City city) {
    if (city == null) {
      return;
    }
    if (Boolean.TRUE.equals(city.getArchived())
        || city.getState() == null
        || Boolean.TRUE.equals(city.getState().getArchived())
        || city.getState().getCountry() == null
        || Boolean.TRUE.equals(city.getState().getCountry().getArchived())) {
      throw inconsistency("Branch city must belong to an active geographic hierarchy.");
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
