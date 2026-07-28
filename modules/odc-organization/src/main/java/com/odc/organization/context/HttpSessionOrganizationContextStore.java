package com.odc.organization.context;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;

public class HttpSessionOrganizationContextStore implements OrganizationContextStore {
  private static final String COMPANY_ID = "odc.organization.companyId";
  private static final String BRANCH_ID = "odc.organization.branchId";
  private final Provider<HttpServletRequest> requestProvider;

  @Inject
  public HttpSessionOrganizationContextStore(Provider<HttpServletRequest> requestProvider) {
    this.requestProvider = requestProvider;
  }

  public Optional<Long> getCompanyId() { return value(COMPANY_ID); }
  public void setCompanyId(Long id) { session().setAttribute(COMPANY_ID, id); }
  public void clearCompanyId() { session().removeAttribute(COMPANY_ID); }
  public Optional<Long> getBranchId() { return value(BRANCH_ID); }
  public void setBranchId(Long id) { session().setAttribute(BRANCH_ID, id); }
  public void clearBranchId() { session().removeAttribute(BRANCH_ID); }
  public void clear() { clearBranchId(); clearCompanyId(); }

  private Optional<Long> value(String key) {
    Object value = session().getAttribute(key);
    return value instanceof Long id ? Optional.of(id) : Optional.empty();
  }

  private HttpSession session() { return requestProvider.get().getSession(); }
}
