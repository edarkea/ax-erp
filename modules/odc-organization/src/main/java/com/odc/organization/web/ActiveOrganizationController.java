package com.odc.organization.web;

import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.odc.organization.db.OrganizationContextSelector;
import com.odc.organization.service.ActiveOrganizationService;
import java.util.List;
import java.util.stream.Collectors;

public class ActiveOrganizationController {
  public void apply(ActionRequest request, ActionResponse response) {
    OrganizationContextSelector selector =
        request.getContext().asType(OrganizationContextSelector.class);
    ActiveOrganizationService service = Beans.get(ActiveOrganizationService.class);
    if (selector.getCompany() == null) service.clearContext();
    else {
      service.setActiveCompany(selector.getCompany());
      if (selector.getBranch() != null) service.setActiveBranch(selector.getBranch());
    }
    load(request, response);
  }

  public void load(ActionRequest request, ActionResponse response) {
    ActiveOrganizationService service = Beans.get(ActiveOrganizationService.class);
    com.odc.organization.db.Company company = service.getActiveCompany().orElse(null);
    com.odc.organization.db.Branch branch =
        company == null ? null : service.getActiveBranch().orElse(null);
    response.setValue("company", company);
    response.setValue("branch", branch);
    response.setAttr("company", "domain", idDomain(service.getAvailableCompanies()));
    response.setAttr("branch", "domain",
        company == null ? "self.id = 0" : idDomain(service.getAvailableBranches(company)));
  }

  public void companyChanged(ActionRequest request, ActionResponse response) {
    OrganizationContextSelector selector =
        request.getContext().asType(OrganizationContextSelector.class);
    response.setValue("branch", null);
    if (selector.getCompany() == null) {
      response.setAttr("branch", "domain", "self.id = 0");
      return;
    }
    response.setAttr(
        "branch",
        "domain",
        idDomain(Beans.get(ActiveOrganizationService.class)
            .getAvailableBranches(selector.getCompany())));
  }

  public void clearBranch(ActionRequest request, ActionResponse response) {
    Beans.get(ActiveOrganizationService.class).clearActiveBranch();
    response.setValue("branch", null);
  }

  public void clearContext(ActionRequest request, ActionResponse response) {
    Beans.get(ActiveOrganizationService.class).clearContext();
    response.setValue("company", null);
    response.setValue("branch", null);
  }

  private String idDomain(List<? extends com.axelor.db.Model> records) {
    if (records.isEmpty()) return "self.id = 0";
    return "self.id IN ("
        + records.stream().map(record -> record.getId().toString()).collect(Collectors.joining(","))
        + ")";
  }

}
