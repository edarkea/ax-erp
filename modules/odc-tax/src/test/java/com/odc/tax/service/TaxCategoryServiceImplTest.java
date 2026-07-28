package com.odc.tax.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odc.reference.db.Country;
import com.odc.tax.db.TaxCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TaxCategoryServiceImplTest {

  private TestTaxCategoryService service;

  @BeforeEach
  void setUp() {
    service = new TestTaxCategoryService();
  }

  @Test
  void shouldCreateAndNormalizeCategory() {
    TaxCategory category = category(country(1L, false), " iva ", "  IVA general  ", "VAT");

    TaxCategory saved = service.save(category);

    assertEquals("IVA", saved.getCode());
    assertEquals("IVA general", saved.getName());
    assertFalse(saved.getArchived());
    assertTrue(service.persisted);
  }

  @Test
  void shouldRejectMissingOrArchivedCountry() {
    assertThrows(
        IllegalArgumentException.class,
        () -> service.validate(category(null, "IVA", "IVA", "VAT")));
    assertThrows(
        IllegalArgumentException.class,
        () -> service.validate(category(country(1L, true), "IVA", "IVA", "VAT")));
  }

  @Test
  void shouldRejectDuplicateInsideSameCountryAndType() {
    service.duplicate = category(country(1L, false), "IVA", "IVA", "VAT");

    assertThrows(
        IllegalArgumentException.class,
        () -> service.validate(category(country(1L, false), " iva ", "IVA", "VAT")));
  }

  @Test
  void shouldAllowSameCodeInAnotherCountryOrType() {
    TaxCategory category = category(country(2L, false), "IVA", "IVA", "WITHHOLDING");

    service.validate(category);

    assertEquals("IVA", category.getCode());
  }

  @Test
  void shouldRejectCountryOrTypeChangeWhenRatesExist() {
    TaxCategory category = category(country(2L, false), "IVA", "IVA", "VAT");
    category.setId(10L);
    service.ratesExist = true;
    service.persistedStructure = new Object[] {1L, "VAT"};
    assertThrows(IllegalArgumentException.class, () -> service.validate(category));

    category.setCountry(country(1L, false));
    category.setType("EXCISE");
    assertThrows(IllegalArgumentException.class, () -> service.validate(category));
  }

  @Test
  void shouldArchiveWithoutPhysicalRemoval() {
    TaxCategory category = category(country(1L, false), "IVA", "IVA", "VAT");
    category.setId(10L);

    service.archive(category);

    assertTrue(category.getArchived());
    assertTrue(service.persisted);
  }

  @Test
  void shouldRevalidateUniquenessWhenRestored() {
    TaxCategory category = category(country(1L, false), "IVA", "IVA", "VAT");
    category.setId(10L);
    category.setArchived(false);
    service.duplicate = category(country(1L, false), "IVA", "Other", "VAT");

    assertThrows(IllegalArgumentException.class, () -> service.validate(category));
  }

  @Test
  void shouldNotExposeCompanyField() {
    assertNull(
        java.util.Arrays.stream(TaxCategory.class.getMethods())
            .filter(method -> method.getName().equals("getCompany"))
            .findFirst()
            .orElse(null));
  }

  private static Country country(Long id, boolean archived) {
    Country country = new Country();
    country.setId(id);
    country.setCode("C" + id);
    country.setArchived(archived);
    return country;
  }

  private static TaxCategory category(
      Country country, String code, String name, String type) {
    TaxCategory category = new TaxCategory();
    category.setCountry(country);
    category.setCode(code);
    category.setName(name);
    category.setType(type);
    return category;
  }

  private static class TestTaxCategoryService extends TaxCategoryServiceImpl {

    private TaxCategory duplicate;
    private boolean ratesExist;
    private Object[] persistedStructure;
    private boolean persisted;

    private TestTaxCategoryService() {
      super(null);
    }

    @Override
    protected TaxCategory findOtherActive(TaxCategory category) {
      if (duplicate == null) {
        return null;
      }
      boolean sameCountry =
          duplicate.getCountry().getId().equals(category.getCountry().getId());
      boolean sameType = duplicate.getType().equals(category.getType());
      return sameCountry && sameType ? duplicate : null;
    }

    @Override
    protected boolean hasRates(TaxCategory category) {
      return ratesExist;
    }

    @Override
    protected Object[] findPersistedStructure(Long id) {
      return persistedStructure;
    }

    @Override
    protected TaxCategory persist(TaxCategory category) {
      persisted = true;
      return category;
    }
  }
}
