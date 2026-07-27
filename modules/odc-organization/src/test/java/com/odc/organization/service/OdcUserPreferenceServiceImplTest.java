package com.odc.organization.service;

import static org.junit.jupiter.api.Assertions.*;

import com.axelor.auth.db.User;
import com.odc.organization.db.OdcUserPreference;
import org.junit.jupiter.api.Test;

class OdcUserPreferenceServiceImplTest {

  @Test
  void shouldCreateGetAndUpdateSinglePreference() {
    TestService service = new TestService();
    User user = user();
    OdcUserPreference created = service.getOrCreate(user);
    assertEquals("SYSTEM", created.getTheme());
    assertEquals("SIDEBAR", created.getNavigationMode());
    service.existing = created;
    assertSame(created, service.getOrCreate(user));
    created.setTheme("DARK");
    created.setLocale("es_EC");
    assertSame(created, service.save(created));
    assertEquals("es-EC", created.getLocale());
    service.archive(created);
    assertTrue(created.getArchived());
  }

  @Test
  void shouldRejectDuplicateInvalidLocaleAndInvalidSelection() {
    TestService service = new TestService();
    OdcUserPreference preference = preference(user());
    OdcUserPreference other = preference(preference.getUser());
    other.setId(2L);
    service.existing = other;
    assertThrows(IllegalArgumentException.class, () -> service.validate(preference));
    service.existing = null;
    preference.setLocale("@invalid");
    assertThrows(IllegalArgumentException.class, () -> service.validate(preference));
    preference.setLocale("es-EC");
    preference.setTheme("UNKNOWN");
    assertThrows(IllegalArgumentException.class, () -> service.validate(preference));
  }

  private static OdcUserPreference preference(User user) {
    OdcUserPreference p = new OdcUserPreference();
    p.setUser(user); p.setTheme("SYSTEM"); p.setNavigationMode("SIDEBAR"); p.setArchived(false);
    return p;
  }
  private static User user() {
    User u = new User("u", "u"); u.setArchived(false); u.setBlocked(false); return u;
  }
  private static class TestService extends OdcUserPreferenceServiceImpl {
    OdcUserPreference existing;
    TestService() {
      super(null, new OrganizationValidationServiceImpl(), new AccessValidationService());
    }
    @Override protected OdcUserPreference findByUser(User u) { return existing; }
    @Override protected OdcUserPreference findAnyByUser(User u) { return existing; }
    @Override protected OdcUserPreference persist(OdcUserPreference p) { return p; }
  }
}
