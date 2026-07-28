package com.odc.accounting.module;

import com.axelor.app.AxelorModule;
import com.odc.accounting.observer.AccountingSaveObserver;
import com.odc.accounting.service.*;

public class OdcAccountingModule extends AxelorModule {
  @Override
  protected void configure() {
    bind(AccountingConfigurationValidationService.class)
        .to(AccountingConfigurationValidationServiceImpl.class);
    bind(ChartAccountService.class).to(ChartAccountServiceImpl.class);
    bind(AccountingRoleDefinitionService.class).to(AccountingRoleDefinitionServiceImpl.class);
    bind(AccountingRoleDefinitionSeedService.class)
        .to(AccountingRoleDefinitionSeedServiceImpl.class);
    bind(AccountingSetupEntryService.class).to(AccountingSetupEntryServiceImpl.class);
    bind(AccountingSetupResolver.class).to(AccountingSetupResolverImpl.class);
    bind(AccountingSaveObserver.class);
  }
}
