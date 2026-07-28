package com.odc.organization.service;

import static org.junit.jupiter.api.Assertions.*;

import com.axelor.auth.db.User;
import com.odc.organization.context.CurrentUserProvider;
import com.odc.organization.context.OrganizationContextStoreTest.MemoryStore;
import com.odc.organization.db.Branch;
import com.odc.organization.db.Company;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ActiveOrganizationServiceImplTest {
  MemoryStore store;
  FakeAccess access;
  User user;
  TestService service;
  Company a;
  Company b;
  Branch a1;
  Branch b1;

  @BeforeEach
  void setup() {
    store = new MemoryStore();
    access = new FakeAccess();
    user = new User("u", "u");
    a = company(1, "A"); b = company(2, "B");
    a1 = branch(11, "A1", a); b1 = branch(21, "B1", b);
    service = new TestService(store, () -> Optional.ofNullable(user), access);
    service.companies.put(1L, a); service.companies.put(2L, b);
    service.branches.put(11L, a1); service.branches.put(21L, b1);
  }

  @Test
  void shouldHandleUnauthenticatedUser() {
    user = null;
    assertTrue(service.getActiveCompany().isEmpty());
    assertThrows(IllegalArgumentException.class, service::requireActiveCompany);
  }

  @Test
  void shouldResolveStoredDefaultAndOnlyCompany() {
    access.companies = List.of(a);
    store.setCompanyId(1L);
    assertSame(a, service.getActiveCompany().orElseThrow());
    store.clear();
    service.defaultCompanies = List.of(a);
    assertSame(a, service.getActiveCompany().orElseThrow());
    service.defaultCompanies = List.of();
    assertSame(a, service.getActiveCompany().orElseThrow());
  }

  @Test
  void shouldClearInvalidStoredCompanyAndRejectAmbiguousOrMissingAccess() {
    store.setCompanyId(2L);
    access.companies = List.of(a);
    assertSame(a, service.getActiveCompany().orElseThrow());
    access.companies = List.of(a, b);
    store.clear();
    assertTrue(service.getActiveCompany().isEmpty());
    assertThrows(IllegalArgumentException.class, service::requireActiveCompany);
    access.companies = List.of();
    assertThrows(IllegalArgumentException.class, service::requireActiveCompany);
  }

  @Test
  void shouldRejectMultipleDefaultCompanies() {
    access.companies = List.of(a, b);
    service.defaultCompanies = List.of(a, b);
    assertThrows(IllegalArgumentException.class, service::getActiveCompany);
  }

  @Test
  void shouldSetCompanyOnlyWithAccessAndPreservePersistentDefault() {
    access.companies = List.of(a);
    assertSame(a, service.setActiveCompany(a));
    assertEquals(Optional.of(1L), store.getCompanyId());
    access.companies = List.of();
    assertThrows(IllegalArgumentException.class, () -> service.setActiveCompany(b));
  }

  @Test
  void shouldResolveStoredDefaultAndOnlyBranchWithinCompany() {
    access.companies = List.of(a);
    access.branches = List.of(a1);
    service.setActiveCompany(a);
    store.setBranchId(11L);
    assertSame(a1, service.getActiveBranch().orElseThrow());
    store.clearBranchId();
    service.defaultBranches = List.of(a1);
    assertSame(a1, service.getActiveBranch().orElseThrow());
  }

  @Test
  void shouldRejectBranchFromOtherCompanyOrWithoutAccess() {
    access.companies = List.of(a, b);
    access.branches = List.of(a1);
    service.setActiveCompany(a);
    assertThrows(IllegalArgumentException.class, () -> service.setActiveBranch(b1));
    access.branches = List.of();
    assertThrows(IllegalArgumentException.class, () -> service.setActiveBranch(a1));
  }

  @Test
  void shouldClearIncompatibleBranchWhenCompanyChanges() {
    access.companies = List.of(a, b);
    access.branches = List.of(a1);
    service.setActiveCompany(a);
    service.setActiveBranch(a1);
    access.branches = List.of();
    service.setActiveCompany(b);
    assertTrue(store.getBranchId().isEmpty());
  }

  @Test
  void shouldKeepCompanyWhenClearingBranchAndClearBothWithCompany() {
    store.setCompanyId(1L); store.setBranchId(11L);
    service.clearActiveBranch();
    assertEquals(Optional.of(1L), store.getCompanyId());
    service.clearActiveCompany();
    assertTrue(store.getCompanyId().isEmpty());
    assertTrue(store.getBranchId().isEmpty());
  }

  private Company company(long id, String code) {
    Company c = new Company(); c.setId(id); c.setCode(code);
    c.setActive(true); c.setArchived(false); return c;
  }
  private Branch branch(long id, String code, Company company) {
    Branch b = new Branch(); b.setId(id); b.setCode(code); b.setCompany(company);
    b.setActive(true); b.setArchived(false); return b;
  }

  static class TestService extends ActiveOrganizationServiceImpl {
    Map<Long, Company> companies = new HashMap<>();
    Map<Long, Branch> branches = new HashMap<>();
    List<Company> defaultCompanies = List.of();
    List<Branch> defaultBranches = List.of();
    TestService(MemoryStore s, CurrentUserProvider u, OrganizationAccessService a) {
      super(s, u, a, null, null, null, null);
    }
    protected Company findCompany(Long id) { return companies.get(id); }
    protected Branch findBranch(Long id) { return branches.get(id); }
    protected List<Company> findDefaultCompanies(User u) { return defaultCompanies; }
    protected List<Branch> findDefaultBranches(User u, Company c) { return defaultBranches; }
  }

  static class FakeAccess implements OrganizationAccessService {
    List<Company> companies = List.of(); List<Branch> branches = List.of();
    public List<Company> findAccessibleCompanies(User u) { return companies; }
    public List<Branch> findAccessibleBranches(User u, Company c) {
      return branches.stream().filter(b -> Objects.equals(b.getCompany().getId(), c.getId())).toList();
    }
    public boolean hasCompanyAccess(User u, Company c) { return companies.contains(c); }
    public boolean hasBranchAccess(User u, Branch b) { return branches.contains(b) && companies.contains(b.getCompany()); }
    public void requireCompanyAccess(User u, Company c) {
      if (!hasCompanyAccess(u, c)) throw new IllegalArgumentException();
    }
    public void requireBranchAccess(User u, Branch b) {
      if (!hasBranchAccess(u, b)) throw new IllegalArgumentException();
    }
    public void grantCompanyAccess(User u, Company c) {}
    public void revokeCompanyAccess(User u, Company c) {}
    public void grantBranchAccess(User u, Branch b) {}
    public void revokeBranchAccess(User u, Branch b) {}
  }
}
