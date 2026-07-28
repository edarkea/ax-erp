package com.odc.tax.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odc.tax.db.TaxCategory;
import com.odc.tax.db.TaxRate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TaxCalculationServiceImplTest {

  @Test
  void shouldCalculateHumanPercentageWithoutFloatingPoint() {
    TaxCalculationService service = new TaxCalculationServiceImpl(null);

    assertEquals(
        new BigDecimal("15.00"),
        service.calculateTax(
            new BigDecimal("100.00"), new BigDecimal("15.0000"), 2, RoundingMode.HALF_UP));
    assertEquals(
        new BigDecimal("0.0000"),
        service.calculateTax(
            new BigDecimal("999.99"), BigDecimal.ZERO, 4, RoundingMode.HALF_UP));
    assertEquals(
        new BigDecimal("-15.00"),
        service.calculateTax(
            new BigDecimal("-100.00"), new BigDecimal("15"), 2, RoundingMode.HALF_UP));
  }

  @Test
  void shouldHonorScaleAndRoundingMode() {
    TaxCalculationService service = new TaxCalculationServiceImpl(null);

    assertEquals(
        new BigDecimal("0.13"),
        service.calculateTax(BigDecimal.ONE, new BigDecimal("12.5"), 2, RoundingMode.HALF_UP));
    assertEquals(
        new BigDecimal("0.12"),
        service.calculateTax(BigDecimal.ONE, new BigDecimal("12.5"), 2, RoundingMode.HALF_EVEN));
    assertEquals(
        new BigDecimal("0.1235"),
        service.calculateTax(
            BigDecimal.ONE, new BigDecimal("12.3456"), 4, RoundingMode.HALF_UP));
  }

  @Test
  void shouldRejectNegativeRateInvalidScaleAndMissingRounding() {
    TaxCalculationService service = new TaxCalculationServiceImpl(null);

    assertThrows(
        IllegalArgumentException.class,
        () -> service.calculateTax(BigDecimal.ONE, new BigDecimal("-1"), 2, RoundingMode.HALF_UP));
    assertThrows(
        IllegalArgumentException.class,
        () -> service.calculateTax(BigDecimal.ONE, BigDecimal.ONE, -1, RoundingMode.HALF_UP));
    assertThrows(
        IllegalArgumentException.class,
        () -> service.calculateTax(BigDecimal.ONE, BigDecimal.ONE, 13, RoundingMode.HALF_UP));
    assertThrows(
        IllegalArgumentException.class,
        () -> service.calculateTax(BigDecimal.ONE, BigDecimal.ONE, 2, null));
  }

  @Test
  void shouldCalculateUsingApplicableCategoryRate() {
    TaxCategory category = new TaxCategory();
    TaxRate rate = new TaxRate();
    rate.setRate(new BigDecimal("15.0000"));
    TaxCalculationService service =
        new TaxCalculationServiceImpl(new FixedTaxRateService(Optional.of(rate)));

    assertEquals(
        new BigDecimal("30.00"),
        service.calculateTax(
            new BigDecimal("200"), category, LocalDate.of(2026, 1, 1), 2,
            RoundingMode.HALF_UP));
  }

  @Test
  void shouldFailWhenNoApplicableRateExists() {
    TaxCalculationService service =
        new TaxCalculationServiceImpl(new FixedTaxRateService(Optional.empty()));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.calculateTax(
                BigDecimal.ONE, new TaxCategory(), LocalDate.now(), 2, RoundingMode.HALF_UP));
  }

  @Test
  void shouldHandleLargeDecimalValues() {
    TaxCalculationService service = new TaxCalculationServiceImpl(null);

    assertEquals(
        new BigDecimal("123456789012345.6789"),
        service.calculateTax(
            new BigDecimal("987654312098765.4312"),
            new BigDecimal("12.5000"),
            4,
            RoundingMode.HALF_UP));
  }

  private static class FixedTaxRateService implements TaxRateService {
    private final Optional<TaxRate> rate;

    private FixedTaxRateService(Optional<TaxRate> rate) {
      this.rate = rate;
    }

    @Override
    public TaxRate requireApplicableRate(TaxCategory taxCategory, LocalDate date) {
      return rate.orElseThrow(() -> new IllegalArgumentException("No applicable rate"));
    }

    @Override
    public Optional<TaxRate> findApplicableRate(TaxCategory category, LocalDate date) {
      return rate;
    }

    @Override
    public TaxRate save(TaxRate taxRate) {
      return taxRate;
    }

    @Override
    public void validate(TaxRate taxRate) {}

    @Override
    public TaxRate createRate(
        TaxCategory category, BigDecimal rate, LocalDate from, LocalDate until) {
      throw new UnsupportedOperationException();
    }

    @Override
    public TaxRate closeRate(TaxRate taxRate, LocalDate until) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void archive(TaxRate taxRate) {}
  }
}
