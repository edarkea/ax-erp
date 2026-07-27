package com.odc.organization.web;

import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.odc.organization.db.Branch;
import com.odc.organization.db.Company;
import com.odc.organization.service.BranchService;
import com.odc.organization.service.CompanyService;

public class OrganizationController {

  public void validateCompany(ActionRequest request, ActionResponse response) {
    Company company = request.getContext().asType(Company.class);
    Beans.get(CompanyService.class).validate(company);
    response.setValue("code", company.getCode());
    response.setValue("name", company.getName());
    response.setValue("defaultCurrency", company.getDefaultCurrency());
    response.setValue("timezone", company.getTimezone());
    response.setValue("locale", company.getLocale());
    response.setValue("active", company.getActive());
    response.setValue("archived", company.getArchived());
  }

  public void validateBranch(ActionRequest request, ActionResponse response) {
    Branch branch = request.getContext().asType(Branch.class);
    Beans.get(BranchService.class).validate(branch);
    response.setValue("code", branch.getCode());
    response.setValue("name", branch.getName());
    response.setValue("isDefault", branch.getIsDefault());
    response.setValue("active", branch.getActive());
    response.setValue("archived", branch.getArchived());
  }

  public void proposeDefaultCurrency(ActionRequest request, ActionResponse response) {
    Company company = request.getContext().asType(Company.class);
    if (company.getDefaultCurrency() == null && company.getCountry() != null) {
      response.setValue("defaultCurrency", company.getCountry().getDefaultCurrency());
    }
  }

  public void setDefaultBranch(ActionRequest request, ActionResponse response) {
    Branch branch = request.getContext().asType(Branch.class);
    Beans.get(BranchService.class).setDefault(branch);
    response.setReload(true);
  }
}
