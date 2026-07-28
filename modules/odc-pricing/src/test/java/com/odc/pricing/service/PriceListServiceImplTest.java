package com.odc.pricing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odc.organization.context.ActiveOrganizationContext;
import com.odc.organization.db.Branch;
import com.odc.organization.db.Company;
import com.odc.organization.service.ActiveOrganizationService;
import com.odc.pricing.db.PriceList;
import com.odc.reference.db.Currency;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PriceListServiceImplTest {

  private Company activeCompany;
  private TestPriceListService service;

  @BeforeEach
  void setUp() {
    activeCompany = company(1L);
    service = new TestPriceListService(activeCompany);
  }

  @Test
  void shouldCreateListWithActiveCompanyAndNormalizeName() {
    PriceList priceList = priceList(null, "  Retail  ", currency(false));

    PriceList saved = service.save(priceList);

    assertSame(activeCompany, saved.getCompany());
    assertEquals("Retail", saved.getName());
    assertTrue(saved.getActive());
    assertFalse(saved.getArchived());
  }

  @Test
  void shouldRejectMissingActiveCompany() {
    service = new TestPriceListService(null);

    assertThrows(
        IllegalArgumentException.class,
        () -> service.save(priceList(null, "Retail", currency(false))));
  }

  @Test
  void shouldRejectManipulatedCompanyOnExistingRecord() {
    PriceList priceList = priceList(company(2L), "Retail", currency(false));
    priceList.setId(10L);

    assertThrows(IllegalArgumentException.class, () -> service.validate(priceList));
  }

  @Test
  void shouldRejectBlankNameAndNormalizeValidName() {
    assertThrows(
        IllegalArgumentException.class,
        () -> service.validate(priceList(activeCompany, "  ", currency(false))));

    PriceList priceList = priceList(activeCompany, "  Wholesale  ", currency(false));
    service.validate(priceList);
    assertEquals("Wholesale", priceList.getName());
  }

  @Test
  void shouldRejectDuplicateNameInsideSameCompany() {
    service.duplicate = priceList(activeCompany, "Retail", currency(false));

    assertThrows(
        IllegalArgumentException.class,
        () -> service.validate(priceList(activeCompany, "Retail", currency(false))));
  }

  @Test
  void shouldAllowSameNameInDifferentCompany() {
    service.duplicate = priceList(company(2L), "Retail", currency(false));

    service.validate(priceList(activeCompany, "Retail", currency(false)));
  }

  @Test
  void shouldRejectMissingOrArchivedCurrency() {
    assertThrows(
        IllegalArgumentException.class,
        () -> service.validate(priceList(activeCompany, "Retail", null)));
    assertThrows(
        IllegalArgumentException.class,
        () -> service.validate(priceList(activeCompany, "Retail", currency(true))));
  }

  @Test
  void shouldArchiveWithoutPhysicalRemoval() {
    PriceList priceList = priceList(activeCompany, "Retail", currency(false));
    priceList.setId(10L);

    service.archive(priceList);

    assertTrue(priceList.getArchived());
    assertFalse(priceList.getActive());
    assertSame(priceList, service.persisted);
  }

  @Test
  void shouldRestoreAndRevalidateUniqueness() {
    PriceList priceList = priceList(activeCompany, "Retail", currency(false));
    priceList.setId(10L);
    priceList.setArchived(true);
    service.duplicate = priceList(activeCompany, "Retail", currency(false));

    assertThrows(IllegalArgumentException.class, () -> service.restore(priceList));
  }

  @Test
  void shouldRejectCompanyChangeForPersistedList() {
    PriceList priceList = priceList(activeCompany, "Retail", currency(false));
    priceList.setId(10L);
    service.persistedCompany = company(2L);

    assertThrows(IllegalArgumentException.class, () -> service.validate(priceList));
  }

  @Test
  void shouldRejectArchivedOrInactiveListForNewOperations() {
    PriceList priceList = priceList(activeCompany, "Retail", currency(false));
    priceList.setArchived(true);
    assertThrows(IllegalArgumentException.class, () -> service.requireUsable(priceList));

    priceList.setArchived(false);
    priceList.setActive(false);
    assertThrows(IllegalArgumentException.class, () -> service.requireUsable(priceList));
  }

  private static PriceList priceList(Company company, String name, Currency currency) {
    PriceList priceList = new PriceList();
    priceList.setCompany(company);
    priceList.setName(name);
    priceList.setCurrency(currency);
    return priceList;
  }

  private static Company company(Long id) {
    Company company = new Company();
    company.setId(id);
    company.setActive(true);
    company.setArchived(false);
    return company;
  }

  private static Currency currency(boolean archived) {
    Currency currency = new Currency();
    currency.setCode("USD");
    currency.setArchived(archived);
    return currency;
  }

  private static class TestPriceListService extends PriceListServiceImpl {

    private PriceList duplicate;
    private PriceList persisted;
    private Company persistedCompany;

    TestPriceListService(Company activeCompany) {
      super(null, new ActiveOrganizationStub(activeCompany));
    }

    @Override
    protected PriceList findDuplicate(PriceList priceList) {
      if (duplicate == null
          || duplicate.getCompany() == null
          || priceList.getCompany() == null
          || !duplicate.getCompany().getId().equals(priceList.getCompany().getId())) {
        return null;
      }
      return duplicate;
    }

    @Override
    protected Company findPersistedCompany(Long id) {
      return persistedCompany;
    }

    @Override
    protected PriceList persist(PriceList priceList) {
      persisted = priceList;
      return priceList;
    }
  }

  private static class ActiveOrganizationStub implements ActiveOrganizationService {

    private final Company company;

    ActiveOrganizationStub(Company company) {
      this.company = company;
    }

    @Override
    public Optional<Company> getActiveCompany() {
      return Optional.ofNullable(company);
    }

    @Override
    public Company requireActiveCompany() {
      if (company == null) {
        throw new IllegalArgumentException("No active company");
      }
      return company;
    }

    @Override public Optional<Branch> getActiveBranch() { return Optional.empty(); }
    @Override public Branch requireActiveBranch() { throw new UnsupportedOperationException(); }
    @Override public ActiveOrganizationContext getContext() { return null; }
    @Override public Company setActiveCompany(Company value) { return value; }
    @Override public Company setActiveCompany(Long id) { return company; }
    @Override public Branch setActiveBranch(Branch value) { return value; }
    @Override public Branch setActiveBranch(Long id) { return null; }
    @Override public void clearActiveCompany() {}
    @Override public void clearActiveBranch() {}
    @Override public void clearContext() {}
    @Override public List<Company> getAvailableCompanies() {
      return company == null ? List.of() : List.of(company);
    }
    @Override public List<Branch> getAvailableBranches() { return List.of(); }
    @Override public List<Branch> getAvailableBranches(Company value) { return List.of(); }
  }
}
