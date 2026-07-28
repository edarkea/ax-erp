package com.odc.document.service;

import static org.junit.jupiter.api.Assertions.*;

import com.axelor.auth.db.User;
import com.odc.document.db.*;
import com.odc.organization.db.*;
import com.odc.organization.service.AccessValidationService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserPointAssignmentAccessTest {
  private User user;
  private Company company;
  private Branch branch;
  private EmissionEstablishment establishment;
  private PointOfSale point;
  private UserPointAssignment assignment;
  private TestService service;

  @BeforeEach
  void setUp() {
    user = new User(); user.setId(1L); user.setArchived(false); user.setBlocked(false);
    company = new Company(); company.setId(1L); company.setActive(true); company.setArchived(false);
    branch = new Branch(); branch.setId(1L); branch.setCompany(company);
    branch.setActive(true); branch.setArchived(false);
    establishment = new EmissionEstablishment(); establishment.setId(1L);
    establishment.setBranch(branch); establishment.setActive(true); establishment.setArchived(false);
    point = new PointOfSale(); point.setId(1L); point.setEmissionEstablishment(establishment);
    point.setActive(true); point.setArchived(false);
    assignment = new UserPointAssignment(); assignment.setId(1L);
    assignment.setUser(user); assignment.setPointOfSale(point);
    assignment.setActive(true); assignment.setArchived(false);
    service = new TestService(new ConfigurationStub());
    service.assignments = List.of(assignment);
  }

  @Test
  void grantsOnlyActiveAssignmentWithCompanyAndBranchAccess() {
    assertTrue(service.hasUserAccess(user, point));
    assertSame(assignment, service.findActiveAssignment(user, point).orElseThrow());
    assertDoesNotThrow(() -> service.requireUserAccess(user, point));
  }

  @Test
  void rejectsMissingInactiveAndArchivedAssignments() {
    service.assignments = List.of();
    assertFalse(service.hasUserAccess(user, point));
    assertThrows(IllegalArgumentException.class, () -> service.requireUserAccess(user, point));
    assignment.setActive(false); service.assignments = List.of();
    assertFalse(service.hasUserAccess(user, point));
    assignment.setActive(true); assignment.setArchived(true); service.assignments = List.of();
    assertFalse(service.hasUserAccess(user, point));
  }

  @Test
  void rejectsNullInactiveUserAndUnusableHierarchy() {
    assertFalse(service.hasUserAccess(null, point));
    assertFalse(service.hasUserAccess(user, null));
    user.setBlocked(true); assertFalse(service.hasUserAccess(user, point)); user.setBlocked(false);
    point.setActive(false); assertFalse(service.hasUserAccess(user, point)); point.setActive(true);
    establishment.setArchived(true); assertFalse(service.hasUserAccess(user, point)); establishment.setArchived(false);
    branch.setActive(false); assertFalse(service.hasUserAccess(user, point)); branch.setActive(true);
    company.setArchived(true); assertFalse(service.hasUserAccess(user, point));
  }

  @Test
  void rejectsMissingCompanyOrBranchAccess() {
    service.companyAccess = false;
    assertFalse(service.hasUserAccess(user, point));
    service.companyAccess = true; service.branchAccess = false;
    assertFalse(service.hasUserAccess(user, point));
  }

  @Test
  void duplicateActiveAssignmentsRaiseIntegrityError() {
    service.assignments = List.of(assignment, new UserPointAssignment());
    assertThrows(IllegalArgumentException.class, () -> service.findActiveAssignment(user, point));
    assertThrows(IllegalArgumentException.class, () -> service.hasUserAccess(user, point));
  }

  private static class TestService extends UserPointAssignmentServiceImpl {
    List<UserPointAssignment> assignments = List.of();
    boolean companyAccess = true, branchAccess = true;
    TestService(EmissionConfigurationService configuration) {
      super(null, null, null, configuration, new AccessValidationService());
    }
    @Override protected List<UserPointAssignment> findActiveAssignments(User user, PointOfSale point) {
      return assignments;
    }
    @Override protected boolean hasCompanyAccess(UserPointAssignment value, Company company) {
      return companyAccess;
    }
    @Override protected boolean hasBranchAccess(UserPointAssignment value, Branch branch) {
      return branchAccess;
    }
  }

  private static class ConfigurationStub implements EmissionConfigurationService {
    public EmissionEstablishment save(EmissionEstablishment value) { return value; }
    public PointOfSale save(PointOfSale value) { return value; }
    public void validate(EmissionEstablishment value) {}
    public void validate(PointOfSale value) {}
    public void requireUsable(EmissionEstablishment value) {
      if (value == null || Boolean.TRUE.equals(value.getArchived())
          || !Boolean.TRUE.equals(value.getActive())) throw new IllegalArgumentException();
      Branch branch = value.getBranch();
      if (branch == null || Boolean.TRUE.equals(branch.getArchived())
          || !Boolean.TRUE.equals(branch.getActive()) || branch.getCompany() == null
          || Boolean.TRUE.equals(branch.getCompany().getArchived())
          || !Boolean.TRUE.equals(branch.getCompany().getActive())) throw new IllegalArgumentException();
    }
    public void requireUsable(PointOfSale value) {
      if (value == null || Boolean.TRUE.equals(value.getArchived())
          || !Boolean.TRUE.equals(value.getActive())) throw new IllegalArgumentException();
      requireUsable(value.getEmissionEstablishment());
    }
  }
}
