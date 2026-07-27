package com.odc.organization.service;

import static org.junit.jupiter.api.Assertions.*;

import com.axelor.auth.db.User;
import com.odc.organization.db.*;
import org.junit.jupiter.api.Test;

class UserBranchAccessServiceImplTest {

  @Test
  void shouldGrantOnlyWithCompanyAccessAndNoCompanyField() {
    TestService service = new TestService();
    UserBranchAccess access = access(user(), branch(company()));
    service.companyAccess = companyAccess(access);
    assertSame(access, service.save(access));
    assertThrows(NoSuchFieldException.class, () -> UserBranchAccess.class.getDeclaredField("company"));
    service.companyAccess = null;
    assertThrows(IllegalArgumentException.class, () -> service.validate(access));
  }

  @Test
  void shouldRejectArchivedBranchDuplicateAndInactiveDefault() {
    TestService service = new TestService();
    UserBranchAccess access = access(user(), branch(company()));
    service.companyAccess = companyAccess(access);
    access.getBranch().setArchived(true);
    assertThrows(IllegalArgumentException.class, () -> service.validate(access));
    access.getBranch().setArchived(false);
    service.other = access(user(), access.getBranch());
    assertThrows(IllegalArgumentException.class, () -> service.validate(access));
    service.other = null;
    access.setActive(false);
    access.setIsDefault(true);
    assertThrows(IllegalArgumentException.class, () -> service.validate(access));
  }

  @Test
  void shouldReplaceDefaultWhileLockingCompanyAccess() {
    TestService service = new TestService();
    UserBranchAccess next = access(user(), branch(company()));
    service.companyAccess = companyAccess(next);
    UserBranchAccess old = access(next.getUser(), branch(next.getBranch().getCompany()));
    old.setIsDefault(true);
    service.otherDefault = old;
    service.setDefault(next);
    assertFalse(old.getIsDefault());
    assertTrue(next.getIsDefault());
    assertTrue(service.locked);
  }

  @Test
  void shouldNotRestoreWithoutActiveCompanyAccess() {
    TestService service = new TestService();
    UserBranchAccess access = access(user(), branch(company()));
    access.setArchived(true);
    service.companyAccess = null;
    assertThrows(IllegalArgumentException.class, () -> service.activate(access));
  }

  private static UserBranchAccess access(User u, Branch b) {
    UserBranchAccess a = new UserBranchAccess(); a.setUser(u); a.setBranch(b); return a;
  }
  private static User user() {
    User u = new User("u", "u"); u.setArchived(false); u.setBlocked(false); return u;
  }
  private static Company company() {
    Company c = new Company(); c.setActive(true); c.setArchived(false); return c;
  }
  private static Branch branch(Company c) {
    Branch b = new Branch(); b.setCompany(c); b.setActive(true); b.setArchived(false); return b;
  }
  private static UserCompanyAccess companyAccess(UserBranchAccess a) {
    UserCompanyAccess x = new UserCompanyAccess(); x.setUser(a.getUser());
    x.setCompany(a.getBranch().getCompany()); x.setActive(true); x.setArchived(false); return x;
  }

  private static class TestService extends UserBranchAccessServiceImpl {
    UserCompanyAccess companyAccess; UserBranchAccess other; UserBranchAccess otherDefault;
    boolean locked;
    TestService() { super(null, null, new UsableBranchService(), new AccessValidationService()); }
    @Override protected UserCompanyAccess findCompanyAccess(User u, Company c) { return companyAccess; }
    @Override protected UserBranchAccess findOther(User u, Branch b, Long id) { return other; }
    @Override protected UserBranchAccess findOtherDefault(User u, Company c, Long id) {
      return otherDefault;
    }
    @Override protected void lockCompanyAccess(UserCompanyAccess a) { locked = true; }
    @Override protected UserBranchAccess persist(UserBranchAccess a) { return a; }
  }

  private static class UsableBranchService implements BranchService {
    public Branch save(Branch b) { return b; } public void validate(Branch b) {}
    public void validate(Branch b, Company c) {} public void setDefault(Branch b) {}
    public void archive(Branch b) {}
    public void requireUsable(Branch b) {
      if (b == null || Boolean.TRUE.equals(b.getArchived()) || !Boolean.TRUE.equals(b.getActive())
          || b.getCompany() == null || Boolean.TRUE.equals(b.getCompany().getArchived()))
        throw new IllegalArgumentException();
    }
  }
}
