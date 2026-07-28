package com.odc.accounting.observer;

import static com.odc.common.rpc.RequestEntityUtils.process;

import com.axelor.event.Observes;
import com.axelor.events.PreRequest;
import com.axelor.events.RequestEvent;
import com.axelor.events.StartupEvent;
import com.axelor.events.qualifiers.EntityType;
import com.odc.accounting.db.AccountingRoleDefinition;
import com.odc.accounting.db.AccountingSetupEntry;
import com.odc.accounting.db.ChartAccount;
import com.odc.accounting.service.AccountingRoleDefinitionService;
import com.odc.accounting.service.AccountingRoleDefinitionSeedService;
import com.odc.accounting.service.AccountingSetupEntryService;
import com.odc.accounting.service.ChartAccountService;
import jakarta.inject.Inject;
import jakarta.inject.Named;

public class AccountingSaveObserver {
  private final ChartAccountService accountService;
  private final AccountingRoleDefinitionService roleService;
  private final AccountingSetupEntryService setupService;
  private final AccountingRoleDefinitionSeedService seedService;

  @Inject
  public AccountingSaveObserver(
      ChartAccountService accountService,
      AccountingRoleDefinitionService roleService,
      AccountingSetupEntryService setupService,
      AccountingRoleDefinitionSeedService seedService) {
    this.accountService = accountService;
    this.roleService = roleService;
    this.setupService = setupService;
    this.seedService = seedService;
  }
  public void startup(@Observes StartupEvent event) { seedService.seed(); }
  public void account(
      @Observes @Named(RequestEvent.SAVE) @EntityType(ChartAccount.class) PreRequest event) {
    process(event, ChartAccount.class, accountService::validate);
  }
  public void role(
      @Observes @Named(RequestEvent.SAVE) @EntityType(AccountingRoleDefinition.class)
          PreRequest event) {
    process(event, AccountingRoleDefinition.class, roleService::validate);
  }
  public void setup(
      @Observes @Named(RequestEvent.SAVE) @EntityType(AccountingSetupEntry.class)
          PreRequest event) {
    process(event, AccountingSetupEntry.class, setupService::validate);
  }
}
