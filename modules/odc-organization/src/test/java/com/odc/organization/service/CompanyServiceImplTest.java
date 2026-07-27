package com.odc.organization.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odc.organization.db.Company;
import com.odc.reference.db.Country;
import com.odc.reference.db.Currency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CompanyServiceImplTest {

  private TestCompanyService service;

  @BeforeEach
  void setUp() {
    service = new TestCompanyService();
  }

  @Test
  void shouldCreateAndNormalizeValidCompany() {
    Company company = company(" ec ");
    company.setName("  ODC Ecuador  ");
    company.setTimezone("America/Guayaquil");
    company.setLocale("es_EC");

    Company saved = service.save(company);

    assertSame(company, saved);
    assertEquals("EC", company.getCode());
    assertEquals("ODC Ecuador", company.getName());
    assertEquals("es-EC", company.getLocale());
    assertTrue(company.getActive());
    assertFalse(company.getArchived());
  }

  @Test
  void shouldRejectDuplicateActiveCode() {
    service.duplicate = company("EC");
    assertThrows(IllegalArgumentException.class, () -> service.validate(company(" ec ")));
  }

  @Test
  void shouldAllowCodeWhenArchivedRecordIsNotReturnedByActiveLookup() {
    service.validate(company("EC"));
  }

  @Test
  void shouldRejectArchivedCountry() {
    Company company = company("EC");
    Country country = new Country();
    country.setArchived(true);
    company.setCountry(country);
    assertThrows(IllegalArgumentException.class, () -> service.validate(company));
  }

  @Test
  void shouldRejectArchivedCurrency() {
    Company company = company("EC");
    Currency currency = new Currency();
    currency.setArchived(true);
    company.setDefaultCurrency(currency);
    assertThrows(IllegalArgumentException.class, () -> service.validate(company));
  }

  @Test
  void shouldSuggestCountryDefaultCurrency() {
    Currency currency = new Currency();
    currency.setArchived(false);
    Country country = new Country();
    country.setArchived(false);
    country.setDefaultCurrency(currency);
    Company company = company("EC");
    company.setCountry(country);

    service.validate(company);

    assertSame(currency, company.getDefaultCurrency());
  }

  @Test
  void shouldRejectInvalidTimezone() {
    Company company = company("EC");
    company.setTimezone("Mars/Olympus");
    assertThrows(IllegalArgumentException.class, () -> service.validate(company));
  }

  @Test
  void shouldArchiveWithoutPhysicalDeletion() {
    Company company = company("EC");
    company.setId(1L);

    service.archive(company);

    assertTrue(company.getArchived());
    assertFalse(company.getActive());
    assertSame(company, service.persisted);
  }

  @Test
  void shouldRejectArchivedOrInactiveCompanyForOperations() {
    Company archived = company("A");
    archived.setArchived(true);
    Company inactive = company("B");
    inactive.setArchived(false);
    inactive.setActive(false);

    assertThrows(IllegalArgumentException.class, () -> service.requireUsable(archived));
    assertThrows(IllegalArgumentException.class, () -> service.requireUsable(inactive));
  }

  private Company company(String code) {
    Company company = new Company();
    company.setCode(code);
    company.setName("Company " + code.trim());
    company.setActive(true);
    company.setArchived(false);
    return company;
  }

  private static class TestCompanyService extends CompanyServiceImpl {

    private Company duplicate;
    private Company persisted;

    private TestCompanyService() {
      super(null, null, new OrganizationValidationServiceImpl());
    }

    @Override
    protected Company findOtherActive(String code, Long excludedId) {
      return duplicate;
    }

    @Override
    protected boolean hasActiveBranches(Company company) {
      return false;
    }

    @Override
    protected Company persist(Company company) {
      persisted = company;
      return company;
    }
  }
}
