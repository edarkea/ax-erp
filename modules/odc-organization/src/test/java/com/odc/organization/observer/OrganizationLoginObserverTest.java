package com.odc.organization.observer;

import static org.junit.jupiter.api.Assertions.*;

import com.axelor.auth.db.User;
import com.axelor.events.PostLogin;
import com.odc.organization.context.OrganizationContextResolution;
import com.odc.organization.context.OrganizationContextStatus;
import com.odc.organization.service.ActiveOrganizationServiceImpl;
import java.io.IOException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.junit.jupiter.api.Test;

class OrganizationLoginObserverTest {

  @Test
  void shouldRedirectWhenUserHasNoCompany() throws IOException {
    TestService service = new TestService(OrganizationContextStatus.NO_COMPANY_ACCESS);
    TestObserver observer = new TestObserver(service);
    observer.onLoginSuccess(event());
    assertTrue(observer.redirected);
  }

  @Test
  void shouldRedirectWhenCompanySelectionIsRequired() throws IOException {
    TestService service =
        new TestService(OrganizationContextStatus.COMPANY_SELECTION_REQUIRED);
    TestObserver observer = new TestObserver(service);
    observer.onLoginSuccess(event());
    assertTrue(observer.redirected);
  }

  @Test
  void shouldContinueNormallyWhenContextIsResolved() throws IOException {
    TestService service = new TestService(OrganizationContextStatus.COMPANY_RESOLVED);
    TestObserver observer = new TestObserver(service);
    observer.onLoginSuccess(event());
    assertFalse(observer.redirected);
  }

  private static class TestObserver extends OrganizationLoginObserver {
    boolean redirected;
    TestObserver(TestService service) { super(service, null, null); }
    @Override protected void redirectToContext() { redirected = true; }
  }

  private PostLogin event() {
    return new PostLogin(new UsernamePasswordToken("u", "p"), new User(), null);
  }

  private static class TestService extends ActiveOrganizationServiceImpl {
    private final OrganizationContextStatus status;
    TestService(OrganizationContextStatus status) {
      super(null, null, null, null, null, null, null);
      this.status = status;
    }
    @Override
    public OrganizationContextResolution initializeContextAfterLogin(User user) {
      return new OrganizationContextResolution(status, null, null);
    }
  }
}
