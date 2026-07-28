package com.odc.party.module;

import com.axelor.app.AxelorModule;
import com.odc.party.service.PartyRoleService;
import com.odc.party.service.PartyRoleServiceImpl;
import com.odc.party.service.PartyAddressService;
import com.odc.party.service.PartyAddressServiceImpl;
import com.odc.party.service.PartyContactService;
import com.odc.party.service.PartyContactServiceImpl;
import com.odc.party.service.PartyService;
import com.odc.party.service.PartyServiceImpl;
import com.odc.party.service.PartyTagService;
import com.odc.party.service.PartyTagServiceImpl;
import com.odc.party.observer.PartySaveObserver;

public class OdcPartyModule extends AxelorModule {
  @Override
  protected void configure() {
    bind(PartyService.class).to(PartyServiceImpl.class);
    bind(PartyRoleService.class).to(PartyRoleServiceImpl.class);
    bind(PartyContactService.class).to(PartyContactServiceImpl.class);
    bind(PartyAddressService.class).to(PartyAddressServiceImpl.class);
    bind(PartyTagService.class).to(PartyTagServiceImpl.class);
    bind(PartySaveObserver.class);
  }
}
