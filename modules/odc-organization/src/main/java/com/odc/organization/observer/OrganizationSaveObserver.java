package com.odc.organization.observer;

import static com.odc.common.rpc.RequestEntityUtils.process;

import com.axelor.event.Observes;
import com.axelor.events.PreRequest;
import com.axelor.events.RequestEvent;
import com.axelor.events.qualifiers.EntityType;
import com.odc.organization.db.Branch;
import com.odc.organization.db.Company;
import com.odc.organization.service.BranchService;
import com.odc.organization.service.CompanyService;
import jakarta.inject.Inject;
import jakarta.inject.Named;

public class OrganizationSaveObserver {

  private final CompanyService companyService;
  private final BranchService branchService;

  @Inject
  public OrganizationSaveObserver(CompanyService companyService, BranchService branchService) {
    this.companyService = companyService;
    this.branchService = branchService;
  }

  public void onCompanySave(
      @Observes @Named(RequestEvent.SAVE) @EntityType(Company.class) PreRequest event) {
    process(
        event,
        Company.class,
        company -> {
          companyService.validate(company);
          if (company.getBranches() != null) {
            for (Branch branch : company.getBranches()) {
              branchService.validate(branch, company);
            }
          }
        });
  }

  public void onBranchSave(
      @Observes @Named(RequestEvent.SAVE) @EntityType(Branch.class) PreRequest event) {
    process(event, Branch.class, branchService::validate);
  }
}
