package com.odc.accounting.web;

import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.meta.schema.actions.ActionView;
import com.odc.accounting.db.AccountingSetupEntry;
import com.odc.accounting.db.AccountingPeriod;
import com.odc.accounting.db.ChartAccount;
import com.odc.accounting.service.AccountingPeriodService;
import com.odc.organization.db.Company;
import com.odc.organization.service.ActiveOrganizationService;

public class AccountingController {
  public void openAccounts(ActionRequest request, ActionResponse response) {
    Company company = Beans.get(ActiveOrganizationService.class).requireActiveCompany();
    response.setView(
        ActionView.define("Chart of accounts")
            .model(ChartAccount.class.getName())
            .add("grid", "chart-account-grid")
            .add("form", "chart-account-form")
            .domain("self.archived = false AND self.company.id = " + company.getId())
            .map());
  }

  public void openAccountTree(ActionRequest request, ActionResponse response) {
    Company company = Beans.get(ActiveOrganizationService.class).requireActiveCompany();
    response.setView(
        ActionView.define("Account tree")
            .model(ChartAccount.class.getName())
            .add("tree", "chart-account-tree")
            .add("form", "chart-account-form")
            .domain("self.archived = false AND self.company.id = " + company.getId())
            .map());
  }

  public void openSetup(ActionRequest request, ActionResponse response) {
    Company company = Beans.get(ActiveOrganizationService.class).requireActiveCompany();
    response.setView(
        ActionView.define("Accounting setup")
            .model(AccountingSetupEntry.class.getName())
            .add("grid", "accounting-setup-entry-grid")
            .add("form", "accounting-setup-entry-form")
            .domain("self.archived = false AND self.company.id = " + company.getId())
            .map());
  }

  public void openPeriods(ActionRequest request, ActionResponse response) {
    Company company = Beans.get(ActiveOrganizationService.class).requireActiveCompany();
    response.setView(
        ActionView.define("Accounting periods")
            .model(AccountingPeriod.class.getName())
            .add("grid", "accounting-period-grid")
            .add("form", "accounting-period-form")
            .domain("self.company.id = " + company.getId())
            .map());
  }

  public void openPeriod(ActionRequest request, ActionResponse response) {
    Beans.get(AccountingPeriodService.class)
        .open(request.getContext().asType(AccountingPeriod.class));
    response.setReload(true);
  }

  public void closePeriod(ActionRequest request, ActionResponse response) {
    Beans.get(AccountingPeriodService.class)
        .close(request.getContext().asType(AccountingPeriod.class));
    response.setReload(true);
  }

  public void reopenPeriod(ActionRequest request, ActionResponse response) {
    Beans.get(AccountingPeriodService.class)
        .reopen(request.getContext().asType(AccountingPeriod.class));
    response.setReload(true);
  }

  public void archivePeriod(ActionRequest request, ActionResponse response) {
    Beans.get(AccountingPeriodService.class)
        .archive(request.getContext().asType(AccountingPeriod.class));
    response.setReload(true);
  }

  public void restorePeriod(ActionRequest request, ActionResponse response) {
    Beans.get(AccountingPeriodService.class)
        .restore(request.getContext().asType(AccountingPeriod.class));
    response.setReload(true);
  }

  public void configureAccountParentDomain(ActionRequest request, ActionResponse response) {
    ChartAccount account = request.getContext().asType(ChartAccount.class);
    Company company = account.getCompany();
    String domain = company == null || company.getId() == null
        ? "self.id = 0"
        : "self.archived = false AND self.active = true AND self.isPosting = false "
            + "AND self.company.id = " + company.getId();
    if (account.getId() != null) domain += " AND self.id != " + account.getId();
    if (account.getAccountType() != null)
      domain += " AND self.accountType = '" + account.getAccountType() + "'";
    response.setAttr("parent", "domain", domain);
  }

  public void configureSetupDomains(ActionRequest request, ActionResponse response) {
    AccountingSetupEntry entry = request.getContext().asType(AccountingSetupEntry.class);
    Company company = entry.getCompany();
    String id = company == null || company.getId() == null ? null : String.valueOf(company.getId());
    response.setAttr("account", "domain", id == null ? "self.id = 0"
        : "self.archived = false AND self.active = true AND self.isPosting = true "
            + "AND self.company.id = " + id);
    response.setAttr("branch", "domain", id == null ? "self.id = 0"
        : "self.archived = false AND self.active = true AND self.company.id = " + id);
  }
}
