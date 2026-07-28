package com.odc.pricing.web;

import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.odc.organization.db.Company;
import com.odc.organization.service.ActiveOrganizationService;
import com.odc.pricing.db.PriceListItem;

public class PricingController {

  public void configureItemDomain(ActionRequest request, ActionResponse response) {
    PriceListItem line = request.getContext().asType(PriceListItem.class);
    Company company =
        line.getPriceList() == null
            ? Beans.get(ActiveOrganizationService.class).requireActiveCompany()
            : line.getPriceList().getCompany();
    response.setAttr(
        "item",
        "domain",
        company == null
            ? "self.id = 0"
            : "self.archived = false AND self.active = true AND self.company.id = "
                + company.getId());
  }
}
