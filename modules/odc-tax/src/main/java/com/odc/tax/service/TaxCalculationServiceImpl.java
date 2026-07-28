package com.odc.tax.service;

import com.axelor.i18n.I18n;
import com.google.inject.Inject;
import com.odc.tax.db.TaxCategory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class TaxCalculationServiceImpl implements TaxCalculationService {

  static final int MAX_SCALE = 12;
  private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

  private final TaxRateService taxRateService;

  @Inject
  public TaxCalculationServiceImpl(TaxRateService taxRateService) {
    this.taxRateService = taxRateService;
  }

  @Override
  public BigDecimal calculateTax(
      BigDecimal taxableBase, BigDecimal rate, int scale, RoundingMode roundingMode) {
    if (taxableBase == null) {
      throw inconsistency("Taxable base is required.");
    }
    if (rate == null) {
      throw inconsistency("Tax rate is required.");
    }
    if (rate.signum() < 0) {
      throw inconsistency("Tax rate cannot be negative.");
    }
    if (scale < 0 || scale > MAX_SCALE) {
      throw inconsistency("Tax scale must be between 0 and 12.");
    }
    if (roundingMode == null) {
      throw inconsistency("Tax rounding mode is required.");
    }
    return taxableBase.multiply(rate).divide(ONE_HUNDRED).setScale(scale, roundingMode);
  }

  @Override
  public BigDecimal calculateTax(
      BigDecimal taxableBase,
      TaxCategory taxCategory,
      LocalDate date,
      int scale,
      RoundingMode roundingMode) {
    BigDecimal rate = taxRateService.requireApplicableRate(taxCategory, date).getRate();
    return calculateTax(taxableBase, rate, scale, roundingMode);
  }

  private IllegalArgumentException inconsistency(String message) {
    return new IllegalArgumentException(I18n.get(message));
  }
}
