package com.odc.tax.service;

import com.odc.tax.db.TaxCategory;
import com.odc.tax.db.TaxRate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public interface TaxRateService {

  TaxRate save(TaxRate taxRate);

  void validate(TaxRate taxRate);

  TaxRate createRate(
      TaxCategory taxCategory, BigDecimal rate, LocalDate validFrom, LocalDate validUntil);

  TaxRate closeRate(TaxRate taxRate, LocalDate validUntil);

  Optional<TaxRate> findApplicableRate(TaxCategory taxCategory, LocalDate date);

  TaxRate requireApplicableRate(TaxCategory taxCategory, LocalDate date);

  void archive(TaxRate taxRate);
}
