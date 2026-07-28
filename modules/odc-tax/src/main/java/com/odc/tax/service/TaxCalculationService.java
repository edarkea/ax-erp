package com.odc.tax.service;

import com.odc.tax.db.TaxCategory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public interface TaxCalculationService {

  BigDecimal calculateTax(
      BigDecimal taxableBase, BigDecimal rate, int scale, RoundingMode roundingMode);

  BigDecimal calculateTax(
      BigDecimal taxableBase,
      TaxCategory taxCategory,
      LocalDate date,
      int scale,
      RoundingMode roundingMode);
}
