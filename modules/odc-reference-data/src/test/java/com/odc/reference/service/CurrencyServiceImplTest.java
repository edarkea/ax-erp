package com.odc.reference.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odc.reference.db.Currency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CurrencyServiceImplTest {

  private TestCurrencyService service;

  @BeforeEach
  void setUp() {
    service = new TestCurrencyService();
  }

  @Test
  void shouldCreateValidCurrencyAndNormalizeCode() {
    Currency currency = currency(" usd ", 2);

    Currency saved = service.save(currency);

    assertSame(currency, saved);
    assertEquals("USD", saved.getCode());
    assertEquals(2, saved.getDecimalPlaces());
    assertSame(currency, service.persisted);
  }

  @Test
  void shouldRejectDuplicateActiveCode() {
    service.active = currency("USD", 2);

    IllegalArgumentException error =
        assertThrows(IllegalArgumentException.class, () -> service.save(currency("usd", 2)));

    assertTrue(error.getMessage().contains("USD"));
    assertEquals(0, service.persistCount);
  }

  @Test
  void shouldRejectCodeWithInvalidLength() {
    assertThrows(IllegalArgumentException.class, () -> service.save(currency("US", 2)));
    assertThrows(IllegalArgumentException.class, () -> service.save(currency("USDD", 2)));
  }

  @Test
  void shouldRejectDecimalPlacesOutsideRange() {
    assertThrows(IllegalArgumentException.class, () -> service.save(currency("USD", -1)));
    assertThrows(IllegalArgumentException.class, () -> service.save(currency("USD", 7)));
  }

  @Test
  void shouldArchiveWithoutDeleting() {
    Currency currency = currency("USD", 2);
    currency.setId(1L);

    Currency archived = service.archive(currency);

    assertSame(currency, archived);
    assertTrue(archived.getArchived());
    assertSame(currency, service.persisted);
    assertEquals(1, service.persistCount);
  }

  @Test
  void shouldRestoreArchivedCurrencyInsteadOfCreatingDuplicate() {
    Currency requested = currency("usd", 2);
    Currency archived = currency("USD", 0);
    archived.setId(10L);
    archived.setArchived(true);
    service.archived = archived;

    Currency restored = service.save(requested);

    assertSame(archived, restored);
    assertFalse(restored.getArchived());
    assertEquals(2, restored.getDecimalPlaces());
    assertSame(archived, service.persisted);
  }

  private Currency currency(String code, Integer decimalPlaces) {
    Currency currency = new Currency();
    currency.setCode(code);
    currency.setName("Test currency");
    currency.setSymbol("$");
    currency.setDecimalPlaces(decimalPlaces);
    return currency;
  }

  private static class TestCurrencyService extends CurrencyServiceImpl {

    private Currency active;
    private Currency archived;
    private Currency persisted;
    private int persistCount;

    private TestCurrencyService() {
      super(null);
    }

    @Override
    protected Currency findOtherByCode(String code, boolean archived, Long excludedId) {
      return archived ? this.archived : active;
    }

    @Override
    protected Currency persist(Currency currency) {
      persisted = currency;
      persistCount++;
      return currency;
    }
  }
}
