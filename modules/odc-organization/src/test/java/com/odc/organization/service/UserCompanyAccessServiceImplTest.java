package com.odc.organization.service;

import static org.junit.jupiter.api.Assertions.*;

import com.axelor.auth.db.User;
import com.odc.organization.db.Company;
import com.odc.organization.db.UserCompanyAccess;
import org.junit.jupiter.api.Test;

class UserCompanyAccessServiceImplTest {

  @Test
  void shouldGrantNormalizeArchiveAndRestoreWithoutDefault() {
    TestService service = new TestService();
    UserCompanyAccess access = access(user("u"), company("A"));
    assertSame(access, service.save(access));
    assertTrue(access.getActive());
    assertFalse(access.getArchived());
    service.archive(access, true);
    assertTrue(access.getArchived());
    assertFalse(access.getActive());
    service.activate(access);
    assertFalse(access.getArchived());
    assertFalse(access.getIsDefault());
  }

  @Test
  void shouldRejectMissingUserCompanyDisabledUserAndArchivedCompany() {
    TestService service = new TestService();
    assertThrows(IllegalArgumentException.class, () -> service.validate(access(null, company("A"))));
    assertThrows(IllegalArgumentException.class, () -> service.validate(access(user("u"), null)));
    User blocked = user("b");
    blocked.setBlocked(true);
    assertThrows(
        IllegalArgumentException.class, () -> service.validate(access(blocked, company("A"))));
    Company archived = company("X");
    archived.setArchived(true);
    assertThrows(
        IllegalArgumentException.class, () -> service.validate(access(user("u"), archived)));
  }

  @Test
  void shouldRejectDuplicateInactiveDefaultAndSecondDefault() {
    TestService service = new TestService();
    UserCompanyAccess access = access(user("u"), company("A"));
    service.other = access(user("x"), company("X"));
    assertThrows(IllegalArgumentException.class, () -> service.validate(access));
    service.other = null;
    access.setActive(false);
    access.setIsDefault(true);
    assertThrows(IllegalArgumentException.class, () -> service.validate(access));
    access.setActive(true);
    service.otherDefault = access(user("z"), company("Z"));
    assertThrows(IllegalArgumentException.class, () -> service.validate(access));
  }

  @Test
  void shouldReplaceDefaultUnderLock() {
    TestService service = new TestService();
    User user = user("u");
    UserCompanyAccess old = access(user, company("A"));
    old.setIsDefault(true);
    UserCompanyAccess next = access(user, company("B"));
    service.otherDefault = old;
    service.setDefault(next);
    assertFalse(old.getIsDefault());
    assertTrue(next.getIsDefault());
    assertTrue(service.locked);
  }

  private static UserCompanyAccess access(User user, Company company) {
    UserCompanyAccess access = new UserCompanyAccess();
    access.setUser(user);
    access.setCompany(company);
    return access;
  }

  private static User user(String code) {
    User user = new User(code, code);
    user.setArchived(false);
    user.setBlocked(false);
    return user;
  }

  private static Company company(String code) {
    Company company = new Company();
    company.setCode(code);
    company.setActive(true);
    company.setArchived(false);
    return company;
  }

  private static class TestService extends UserCompanyAccessServiceImpl {
    UserCompanyAccess other;
    UserCompanyAccess otherDefault;
    boolean locked;

    TestService() {
      super(null, null, new UsableCompanyService(), new AccessValidationService());
    }

    @Override protected UserCompanyAccess findOther(User u, Company c, Long id) { return other; }
    @Override protected UserCompanyAccess findOtherDefault(User u, Long id) { return otherDefault; }
    @Override protected boolean hasActiveBranchAccesses(UserCompanyAccess a) { return false; }
    @Override protected void lockUser(User user) { locked = true; }
    @Override protected UserCompanyAccess persist(UserCompanyAccess a) { return a; }
  }

  private static class UsableCompanyService implements CompanyService {
    public Company save(Company c) { return c; }
    public void validate(Company c) {}
    public void archive(Company c) {}
    public void requireUsable(Company c) {
      if (c == null || Boolean.TRUE.equals(c.getArchived()) || !Boolean.TRUE.equals(c.getActive()))
        throw new IllegalArgumentException();
    }
  }
}
