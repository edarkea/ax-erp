package com.odc.organization.module;

import com.axelor.app.AxelorModule;
import com.odc.organization.observer.OrganizationSaveObserver;
import com.odc.organization.observer.OrganizationAccessSaveObserver;
import com.odc.organization.observer.OrganizationLoginObserver;
import com.odc.organization.service.AccessValidationService;
import com.odc.organization.service.BranchService;
import com.odc.organization.service.BranchServiceImpl;
import com.odc.organization.service.CompanyService;
import com.odc.organization.service.CompanyServiceImpl;
import com.odc.organization.service.OrganizationValidationService;
import com.odc.organization.service.OrganizationValidationServiceImpl;
import com.odc.organization.service.OrganizationAccessService;
import com.odc.organization.service.OrganizationAccessServiceImpl;
import com.odc.organization.service.UserCompanyAccessService;
import com.odc.organization.service.UserCompanyAccessServiceImpl;
import com.odc.organization.service.UserBranchAccessService;
import com.odc.organization.service.UserBranchAccessServiceImpl;
import com.odc.organization.service.OdcUserPreferenceService;
import com.odc.organization.service.OdcUserPreferenceServiceImpl;
import com.odc.organization.context.AxelorCurrentUserProvider;
import com.odc.organization.context.CurrentUserProvider;
import com.odc.organization.context.HttpSessionOrganizationContextStore;
import com.odc.organization.context.OrganizationContextStore;
import com.odc.organization.service.ActiveOrganizationService;
import com.odc.organization.service.ActiveOrganizationServiceImpl;

public class OdcOrganizationModule extends AxelorModule {

  @Override
  protected void configure() {
    bind(OrganizationValidationService.class).to(OrganizationValidationServiceImpl.class);
    bind(CompanyService.class).to(CompanyServiceImpl.class);
    bind(BranchService.class).to(BranchServiceImpl.class);
    bind(AccessValidationService.class);
    bind(UserCompanyAccessService.class).to(UserCompanyAccessServiceImpl.class);
    bind(UserBranchAccessService.class).to(UserBranchAccessServiceImpl.class);
    bind(OdcUserPreferenceService.class).to(OdcUserPreferenceServiceImpl.class);
    bind(OrganizationAccessService.class).to(OrganizationAccessServiceImpl.class);
    bind(CurrentUserProvider.class).to(AxelorCurrentUserProvider.class);
    bind(OrganizationContextStore.class).to(HttpSessionOrganizationContextStore.class);
    bind(ActiveOrganizationService.class).to(ActiveOrganizationServiceImpl.class);
    bind(OrganizationSaveObserver.class);
    bind(OrganizationAccessSaveObserver.class);
    bind(OrganizationLoginObserver.class);
  }
}
