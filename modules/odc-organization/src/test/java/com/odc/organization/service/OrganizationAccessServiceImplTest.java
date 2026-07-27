package com.odc.organization.service;

import static org.junit.jupiter.api.Assertions.*;

import com.axelor.auth.db.User;
import com.odc.organization.db.Branch;
import com.odc.organization.db.Company;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrganizationAccessServiceImplTest {

  @Test
  void shouldRequireExplicitCompanyAndBranchAccessWithoutAdminBypass() {
    TestService service = new TestService();
    User user = new User("admin", "admin");
    Company company = new Company();
    Branch branch = new Branch();
    branch.setCompany(company);
    assertThrows(IllegalArgumentException.class, () -> service.requireCompanyAccess(user, company));
    assertThrows(IllegalArgumentException.class, () -> service.requireBranchAccess(user, branch));
    service.company = true;
    service.branch = true;
    assertDoesNotThrow(() -> service.requireCompanyAccess(user, company));
    assertDoesNotThrow(() -> service.requireBranchAccess(user, branch));
  }

  @Test
  void shouldExposeAccessibleCollections() {
    TestService service = new TestService();
    assertEquals(1, service.findAccessibleCompanies(new User()).size());
    assertEquals(1, service.findAccessibleBranches(new User(), new Company()).size());
  }

  private static class TestService extends OrganizationAccessServiceImpl {
    boolean company; boolean branch;
    TestService() { super(null, null, null, null, new AccessValidationService()); }
    @Override public boolean hasCompanyAccess(User u, Company c) { return company; }
    @Override public boolean hasBranchAccess(User u, Branch b) { return branch; }
    @Override public List<Company> findAccessibleCompanies(User u) { return List.of(new Company()); }
    @Override public List<Branch> findAccessibleBranches(User u, Company c) {
      return List.of(new Branch());
    }
  }
}
