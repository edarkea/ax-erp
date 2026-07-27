package com.odc.organization.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odc.organization.db.Branch;
import com.odc.organization.db.Company;
import com.odc.reference.db.City;
import com.odc.reference.db.Country;
import com.odc.reference.db.State;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BranchServiceImplTest {

  private TestBranchService service;

  @BeforeEach
  void setUp() {
    service = new TestBranchService(new TestCompanyService());
  }

  @Test
  void shouldCreateAndNormalizeValidBranchWithDirectCompany() {
    Company company = company("A");
    Branch branch = branch(" main ", company);
    branch.setName("  Main branch ");

    Branch saved = service.save(branch);

    assertSame(branch, saved);
    assertSame(company, branch.getCompany());
    assertEquals("MAIN", branch.getCode());
    assertEquals("Main branch", branch.getName());
    assertFalse(branch.getArchived());
    assertTrue(branch.getActive());
  }

  @Test
  void shouldRejectBranchWithoutCompany() {
    assertThrows(IllegalArgumentException.class, () -> service.validate(branch("A", null)));
  }

  @Test
  void shouldRejectDuplicateCodeInsideCompany() {
    Company company = company("C");
    service.duplicate = branch("A", company);
    assertThrows(
        IllegalArgumentException.class, () -> service.validate(branch("A", company)));
  }

  @Test
  void shouldAllowSameCodeInDifferentCompanies() {
    service.validate(branch("A", company("OTHER")));
  }

  @Test
  void shouldRejectArchivedCity() {
    City city = validCity();
    city.setArchived(true);
    Branch branch = branch("A", company("C"));
    branch.setCity(city);
    assertThrows(IllegalArgumentException.class, () -> service.validate(branch));
  }

  @Test
  void shouldRejectArchivedOrInactiveCompany() {
    Company archived = company("A");
    archived.setArchived(true);
    Company inactive = company("B");
    inactive.setActive(false);
    assertThrows(IllegalArgumentException.class, () -> service.validate(branch("A", archived)));
    assertThrows(IllegalArgumentException.class, () -> service.validate(branch("A", inactive)));
  }

  @Test
  void shouldRejectSecondActiveDefaultBranch() {
    service.defaultBranch = branch("OLD", company("C"));
    Branch branch = branch("NEW", company("C"));
    branch.setIsDefault(true);
    assertThrows(IllegalArgumentException.class, () -> service.validate(branch));
  }

  @Test
  void shouldReplaceDefaultTransactionally() {
    Branch current = branch("OLD", company("C"));
    current.setIsDefault(true);
    service.defaultBranch = current;
    Branch replacement = branch("NEW", current.getCompany());

    service.setDefault(replacement);

    assertFalse(current.getIsDefault());
    assertTrue(replacement.getIsDefault());
    assertEquals(2, service.persistCount);
  }

  @Test
  void shouldRejectCompanyChangeWhenOperationallyUsed() {
    Branch persisted = branch("A", company("OLD"));
    persisted.setId(1L);
    Branch changed = branch("A", company("NEW"));
    changed.setId(1L);
    service.used = true;
    service.persisted = persisted;
    assertThrows(IllegalArgumentException.class, () -> service.validate(changed));
  }

  @Test
  void shouldArchiveWithoutPhysicalDeletion() {
    Branch branch = branch("A", company("C"));
    branch.setId(1L);

    service.archive(branch);

    assertTrue(branch.getArchived());
    assertFalse(branch.getActive());
  }

  @Test
  void shouldRejectArchivingLastRequiredActiveBranch() {
    Branch branch = branch("A", company("C"));
    branch.setId(1L);
    service.required = true;
    service.otherActiveCount = 0;
    assertThrows(IllegalArgumentException.class, () -> service.archive(branch));
  }

  private Branch branch(String code, Company company) {
    Branch branch = new Branch();
    branch.setCode(code);
    branch.setName("Branch " + code.trim());
    branch.setCompany(company);
    branch.setActive(true);
    branch.setArchived(false);
    branch.setIsDefault(false);
    return branch;
  }

  private Company company(String code) {
    Company company = new Company();
    company.setCode(code);
    company.setName("Company " + code);
    company.setActive(true);
    company.setArchived(false);
    return company;
  }

  private City validCity() {
    Country country = new Country();
    country.setArchived(false);
    State state = new State();
    state.setArchived(false);
    state.setCountry(country);
    City city = new City();
    city.setArchived(false);
    city.setState(state);
    return city;
  }

  private static class TestBranchService extends BranchServiceImpl {

    private Branch duplicate;
    private Branch defaultBranch;
    private Branch persisted;
    private boolean used;
    private boolean required;
    private long otherActiveCount = 1;
    private int persistCount;

    private TestBranchService(CompanyService companyService) {
      super(null, companyService, new OrganizationValidationServiceImpl());
    }

    @Override
    protected Branch findOtherActive(Company company, String code, Long excludedId) {
      return duplicate != null && duplicate.getCompany() == company ? duplicate : null;
    }

    @Override
    protected Branch findOtherActiveDefault(Company company, Long excludedId) {
      return defaultBranch;
    }

    @Override
    protected long countOtherActive(Branch branch) {
      return otherActiveCount;
    }

    @Override
    protected boolean requiresActiveBranch(Company company) {
      return required;
    }

    @Override
    protected boolean isOperationallyUsed(Branch branch) {
      return used;
    }

    @Override
    protected Branch findPersisted(Long id) {
      return persisted;
    }

    @Override
    protected Branch persist(Branch branch) {
      persistCount++;
      return branch;
    }
  }

  private static class TestCompanyService implements CompanyService {

    @Override
    public Company save(Company company) {
      return company;
    }

    @Override
    public void validate(Company company) {}

    @Override
    public void archive(Company company) {}

    @Override
    public void requireUsable(Company company) {
      if (company == null
          || Boolean.TRUE.equals(company.getArchived())
          || !Boolean.TRUE.equals(company.getActive())) {
        throw new IllegalArgumentException("Company must be usable.");
      }
    }
  }
}
