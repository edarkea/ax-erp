package com.odc.organization.observer;

import static com.odc.common.rpc.RequestEntityUtils.process;

import com.axelor.event.Observes;
import com.axelor.events.PreRequest;
import com.axelor.events.RequestEvent;
import com.axelor.events.qualifiers.EntityType;
import com.odc.organization.db.OdcUserPreference;
import com.odc.organization.db.UserBranchAccess;
import com.odc.organization.db.UserCompanyAccess;
import com.odc.organization.service.OdcUserPreferenceService;
import com.odc.organization.service.UserBranchAccessService;
import com.odc.organization.service.UserCompanyAccessService;
import jakarta.inject.Inject;
import jakarta.inject.Named;

public class OrganizationAccessSaveObserver {
  @Inject UserCompanyAccessService companyService;
  @Inject UserBranchAccessService branchService;
  @Inject OdcUserPreferenceService preferenceService;

  public void company(
      @Observes @Named(RequestEvent.SAVE) @EntityType(UserCompanyAccess.class) PreRequest event) {
    process(event, UserCompanyAccess.class, companyService::validate);
  }

  public void branch(
      @Observes @Named(RequestEvent.SAVE) @EntityType(UserBranchAccess.class) PreRequest event) {
    process(event, UserBranchAccess.class, branchService::validate);
  }

  public void preference(
      @Observes @Named(RequestEvent.SAVE) @EntityType(OdcUserPreference.class) PreRequest event) {
    process(event, OdcUserPreference.class, preferenceService::validate);
  }
}
