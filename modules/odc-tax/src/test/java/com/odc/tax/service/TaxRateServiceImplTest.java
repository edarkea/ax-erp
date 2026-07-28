package com.odc.tax.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odc.reference.db.Country;
import com.odc.tax.db.TaxCategory;
import com.odc.tax.db.TaxRate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TaxRateServiceImplTest {

  private TestTaxRateService service;
  private TaxCategory category;

  @BeforeEach
  void setUp() {
    category = category();
    service = new TestTaxRateService();
  }

  @Test
  void shouldCreateValidAndZeroRates() {
    TaxRate standard =
        service.createRate(
            category, new BigDecimal("15.0000"), date("2026-01-01"), date("2026-06-30"));
    TaxRate zero =
        service.createRate(
            category, BigDecimal.ZERO, date("2026-07-01"), date("2026-12-31"));

    assertEquals(new BigDecimal("15.0000"), standard.getRate());
    assertEquals(BigDecimal.ZERO, zero.getRate());
    assertFalse(standard.getArchived());
  }

  @Test
  void shouldRejectNegativeRateAndInvalidDates() {
    assertThrows(
        IllegalArgumentException.class,
        () -> service.validate(rate(new BigDecimal("-1"), date("2026-01-01"), null)));
    assertThrows(
        IllegalArgumentException.class,
        () -> service.validate(rate(BigDecimal.ONE, null, null)));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.validate(
                rate(BigDecimal.ONE, date("2026-02-01"), date("2026-01-31"))));
  }

  @Test
  void shouldRejectArchivedCategory() {
    category.setArchived(true);
    assertThrows(
        IllegalArgumentException.class,
        () -> service.validate(rate(BigDecimal.ONE, date("2026-01-01"), null)));
  }

  @Test
  void shouldAllowOpenAndConsecutivePeriods() {
    service.validate(rate(BigDecimal.ONE, date("2026-01-01"), null));
    service.validate(
        rate(BigDecimal.ONE, date("2026-07-01"), date("2026-12-31")));
  }

  @Test
  void shouldRejectEveryInclusiveOverlapShape() {
    service.overlap = true;
    List<TaxRate> candidates =
        List.of(
            rate(BigDecimal.ONE, date("2026-03-01"), date("2026-09-01")),
            rate(BigDecimal.ONE, date("2025-01-01"), date("2026-03-01")),
            rate(BigDecimal.ONE, date("2026-04-01"), date("2026-05-01")),
            rate(BigDecimal.ONE, date("2025-01-01"), date("2027-01-01")),
            rate(BigDecimal.ONE, date("2026-01-01"), null),
            rate(BigDecimal.ONE, date("2026-01-01"), date("2026-12-31")),
            rate(BigDecimal.ONE, date("2026-06-30"), null));

    for (TaxRate candidate : candidates) {
      assertThrows(IllegalArgumentException.class, () -> service.validate(candidate));
    }
  }

  @Test
  void shouldResolveInclusiveBoundariesAndIgnoreMissingRate() {
    TaxRate applicable =
        rate(new BigDecimal("15"), date("2026-01-01"), date("2026-06-30"));
    service.applicable = List.of(applicable);

    assertEquals(applicable, service.findApplicableRate(category, date("2026-01-01")).orElseThrow());
    assertEquals(applicable, service.findApplicableRate(category, date("2026-06-30")).orElseThrow());

    service.applicable = List.of();
    assertTrue(service.findApplicableRate(category, date("2027-01-01")).isEmpty());
    assertThrows(
        IllegalArgumentException.class,
        () -> service.requireApplicableRate(category, date("2027-01-01")));
  }

  @Test
  void shouldDetectMultipleApplicableRates() {
    service.applicable =
        List.of(
            rate(BigDecimal.ONE, date("2026-01-01"), null),
            rate(BigDecimal.TEN, date("2026-01-01"), null));

    assertThrows(
        IllegalArgumentException.class,
        () -> service.findApplicableRate(category, date("2026-01-01")));
  }

  @Test
  void shouldCloseRateAndRejectCloseBeforeStart() {
    TaxRate existing = rate(BigDecimal.ONE, date("2026-01-01"), null);
    existing.setId(20L);
    service.lockedRate = existing;

    TaxRate closed = service.closeRate(existing, date("2026-06-30"));
    assertEquals(date("2026-06-30"), closed.getValidUntil());

    assertThrows(
        IllegalArgumentException.class,
        () -> service.closeRate(existing, date("2025-12-31")));
  }

  @Test
  void shouldArchiveWithoutPhysicalRemovalAndValidateRestore() {
    TaxRate existing = rate(BigDecimal.ONE, date("2026-01-01"), null);
    existing.setId(20L);
    service.archive(existing);
    assertTrue(existing.getArchived());

    existing.setArchived(false);
    service.overlap = true;
    assertThrows(IllegalArgumentException.class, () -> service.validate(existing));
  }

  @Test
  void shouldSerializeConcurrentCreationAtCategoryLock() throws Exception {
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      Future<TaxRate> first =
          executor.submit(
              () -> {
                start.await();
                return service.createRate(category, BigDecimal.ONE, date("2026-01-01"), null);
              });
      Future<TaxRate> second =
          executor.submit(
              () -> {
                start.await();
                return service.createRate(category, BigDecimal.TEN, date("2026-01-01"), null);
              });
      start.countDown();
      first.get();
      second.get();

      assertEquals(2, service.lockCount);
      assertEquals(2, service.persisted.size());
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void shouldNotExposeCompanyField() {
    assertFalse(
        java.util.Arrays.stream(TaxRate.class.getMethods())
            .anyMatch(method -> method.getName().equals("getCompany")));
  }

  private TaxRate rate(BigDecimal value, LocalDate from, LocalDate until) {
    TaxRate rate = new TaxRate();
    rate.setTaxCategory(category);
    rate.setRate(value);
    rate.setValidFrom(from);
    rate.setValidUntil(until);
    return rate;
  }

  private static TaxCategory category() {
    Country country = new Country();
    country.setId(1L);
    country.setArchived(false);
    TaxCategory category = new TaxCategory();
    category.setId(10L);
    category.setCountry(country);
    category.setCode("IVA");
    category.setName("IVA");
    category.setType("VAT");
    category.setArchived(false);
    return category;
  }

  private static LocalDate date(String value) {
    return LocalDate.parse(value);
  }

  private static class TestTaxRateService extends TaxRateServiceImpl {

    private boolean overlap;
    private List<TaxRate> applicable = List.of();
    private TaxRate lockedRate;
    private final List<TaxRate> persisted = new ArrayList<>();
    private int lockCount;

    private TestTaxRateService() {
      super(null, new InMemoryCategoryService());
    }

    @Override
    protected synchronized void lockCategory(TaxCategory category) {
      if (category == null || Boolean.TRUE.equals(category.getArchived())) {
        throw new IllegalArgumentException("Tax category is archived.");
      }
      lockCount++;
    }

    @Override
    protected boolean hasOverlap(TaxRate taxRate) {
      return overlap;
    }

    @Override
    protected List<TaxRate> findApplicableRates(TaxCategory category, LocalDate date) {
      return applicable;
    }

    @Override
    protected TaxRate findAndLockRate(Long id) {
      return lockedRate;
    }

    @Override
    protected Object[] findPersistedIdentity(Long id) {
      TaxRate source = lockedRate;
      if (source == null || id == null) {
        return null;
      }
      return new Object[] {
        source.getTaxCategory().getId(), source.getRate(), source.getValidFrom()
      };
    }

    @Override
    protected synchronized TaxRate persist(TaxRate taxRate) {
      persisted.add(taxRate);
      return taxRate;
    }
  }

  private static class InMemoryCategoryService implements TaxCategoryService {
    @Override
    public TaxCategory save(TaxCategory category) {
      return category;
    }

    @Override
    public void validate(TaxCategory category) {}

    @Override
    public void archive(TaxCategory category) {}

    @Override
    public void requireUsable(TaxCategory category) {
      if (category == null || Boolean.TRUE.equals(category.getArchived())) {
        throw new IllegalArgumentException("Tax category is archived.");
      }
    }
  }
}
