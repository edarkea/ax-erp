package com.odc.organization.module;

import com.axelor.app.AxelorModule;
import com.odc.organization.observer.OrganizationSaveObserver;
import com.odc.organization.service.BranchService;
import com.odc.organization.service.BranchServiceImpl;
import com.odc.organization.service.CompanyService;
import com.odc.organization.service.CompanyServiceImpl;
import com.odc.organization.service.OrganizationValidationService;
import com.odc.organization.service.OrganizationValidationServiceImpl;

public class OdcOrganizationModule extends AxelorModule {

  @Override
  protected void configure() {
    bind(OrganizationValidationService.class).to(OrganizationValidationServiceImpl.class);
    bind(CompanyService.class).to(CompanyServiceImpl.class);
    bind(BranchService.class).to(BranchServiceImpl.class);
    bind(OrganizationSaveObserver.class);
  }
}
