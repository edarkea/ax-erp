package com.odc.catalog.web;

import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.odc.catalog.db.Item;
import com.odc.catalog.db.ItemCategory;
import com.odc.organization.db.Company;
import com.odc.organization.service.ActiveOrganizationService;

public class CatalogController {

  public void initializeItem(ActionRequest request, ActionResponse response) {
    Company company = activeCompany();
    response.setValue("company", company);
    configureItemDomains(response, company);
  }

  public void loadItem(ActionRequest request, ActionResponse response) {
    Item item = request.getContext().asType(Item.class);
    configureItemDomains(response, item.getCompany());
  }

  public void initializeCategory(ActionRequest request, ActionResponse response) {
    Company company = activeCompany();
    response.setValue("company", company);
    configureCategoryDomain(response, company, null);
  }

  public void loadCategory(ActionRequest request, ActionResponse response) {
    ItemCategory category = request.getContext().asType(ItemCategory.class);
    configureCategoryDomain(response, category.getCompany(), category.getId());
  }

  private void configureItemDomains(ActionResponse response, Company company) {
    if (company == null) {
      response.setAttr("category", "domain", "self.id = 0");
      response.setAttr("taxCategory", "domain", "self.id = 0");
      return;
    }
    response.setAttr(
        "category",
        "domain",
        "self.archived = false AND self.active = true AND self.company.id = "
            + company.getId());
    response.setAttr(
        "taxCategory",
        "domain",
        company.getCountry() == null
            ? "self.id = 0"
            : "self.archived = false AND self.country.id = " + company.getCountry().getId());
  }

  private void configureCategoryDomain(
      ActionResponse response, Company company, Long categoryId) {
    if (company == null) {
      response.setAttr("parent", "domain", "self.id = 0");
      return;
    }
    String domain =
        "self.archived = false AND self.active = true AND self.company.id = "
            + company.getId();
    if (categoryId != null) {
      domain += " AND self.id != " + categoryId;
    }
    response.setAttr("parent", "domain", domain);
  }

  private Company activeCompany() {
    return Beans.get(ActiveOrganizationService.class).requireActiveCompany();
  }
}
