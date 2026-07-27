package com.odc.organization.observer;

import com.axelor.event.Observes;
import com.axelor.events.PostLogin;
import com.google.inject.servlet.RequestScoped;
import com.odc.organization.context.OrganizationContextStatus;
import com.odc.organization.service.ActiveOrganizationService;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.apache.shiro.web.util.WebUtils;

@RequestScoped
public class OrganizationLoginObserver {
  static final String CONTEXT_ACTION_URL = "/#/ds/action-active-organization-view";

  private final ActiveOrganizationService service;
  private final Provider<HttpServletRequest> requestProvider;
  private final Provider<HttpServletResponse> responseProvider;

  @Inject
  public OrganizationLoginObserver(
      ActiveOrganizationService service,
      Provider<HttpServletRequest> requestProvider,
      Provider<HttpServletResponse> responseProvider) {
    this.service = service;
    this.requestProvider = requestProvider;
    this.responseProvider = responseProvider;
  }

  public void onLoginSuccess(@Observes @Named(PostLogin.SUCCESS) PostLogin event)
      throws IOException {
    OrganizationContextStatus status =
        service.initializeContextAfterLogin(event.getUser()).status();
    if (status == OrganizationContextStatus.NO_COMPANY_ACCESS
        || status == OrganizationContextStatus.COMPANY_SELECTION_REQUIRED) {
      redirectToContext();
    }
  }

  protected void redirectToContext() throws IOException {
    WebUtils.issueRedirect(
        requestProvider.get(), responseProvider.get(), CONTEXT_ACTION_URL);
  }
}
