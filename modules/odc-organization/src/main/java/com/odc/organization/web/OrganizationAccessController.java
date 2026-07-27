package com.odc.organization.web;

import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.odc.organization.db.UserBranchAccess;
import com.odc.organization.db.UserCompanyAccess;
import com.odc.organization.service.UserBranchAccessService;
import com.odc.organization.service.UserCompanyAccessService;

public class OrganizationAccessController {

  public void setDefaultCompany(ActionRequest request, ActionResponse response) {
    Beans.get(UserCompanyAccessService.class)
        .setDefault(request.getContext().asType(UserCompanyAccess.class));
    response.setReload(true);
  }

  public void setDefaultBranch(ActionRequest request, ActionResponse response) {
    Beans.get(UserBranchAccessService.class)
        .setDefault(request.getContext().asType(UserBranchAccess.class));
    response.setReload(true);
  }

  public void activateCompany(ActionRequest request, ActionResponse response) {
    Beans.get(UserCompanyAccessService.class)
        .activate(request.getContext().asType(UserCompanyAccess.class));
    response.setReload(true);
  }

  public void deactivateCompany(ActionRequest request, ActionResponse response) {
    Beans.get(UserCompanyAccessService.class)
        .deactivate(request.getContext().asType(UserCompanyAccess.class), false);
    response.setReload(true);
  }

  public void activateBranch(ActionRequest request, ActionResponse response) {
    Beans.get(UserBranchAccessService.class)
        .activate(request.getContext().asType(UserBranchAccess.class));
    response.setReload(true);
  }

  public void deactivateBranch(ActionRequest request, ActionResponse response) {
    Beans.get(UserBranchAccessService.class)
        .deactivate(request.getContext().asType(UserBranchAccess.class), false);
    response.setReload(true);
  }
}
