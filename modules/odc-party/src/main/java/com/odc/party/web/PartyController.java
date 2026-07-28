package com.odc.party.web;

import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.odc.organization.db.Company;
import com.odc.organization.service.ActiveOrganizationService;
import com.odc.party.db.PartyTagLink;

public class PartyController {

  public void configureTagDomain(ActionRequest request, ActionResponse response) {
    PartyTagLink link = request.getContext().asType(PartyTagLink.class);
    Company company =
        link.getParty() == null
            ? Beans.get(ActiveOrganizationService.class).requireActiveCompany()
            : link.getParty().getCompany();
    response.setAttr(
        "tag",
        "domain",
        company == null
            ? "self.id = 0"
            : "self.archived = false AND self.active = true AND self.company.id = "
                + company.getId());
  }
}
