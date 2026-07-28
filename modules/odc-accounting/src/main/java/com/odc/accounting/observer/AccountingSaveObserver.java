package com.odc.accounting.observer;

import static com.odc.common.rpc.RequestEntityUtils.process;

import com.axelor.event.Observes;
import com.axelor.events.PreRequest;
import com.axelor.events.RequestEvent;
import com.axelor.events.StartupEvent;
import com.axelor.events.qualifiers.EntityType;
import com.odc.accounting.db.AccountingRoleDefinition;
import com.odc.accounting.db.AccountingSetupEntry;
import com.odc.accounting.db.AccountingPeriod;
import com.odc.accounting.db.ChartAccount;
import com.odc.accounting.db.JournalEntry;
import com.odc.accounting.db.JournalLine;
import com.odc.accounting.service.AccountingRoleDefinitionService;
import com.odc.accounting.service.AccountingRoleDefinitionSeedService;
import com.odc.accounting.service.AccountingPeriodService;
import com.odc.accounting.service.AccountingSetupEntryService;
import com.odc.accounting.service.ChartAccountService;
import com.odc.accounting.service.JournalEntryService;
import com.odc.accounting.service.JournalLineService;
import jakarta.inject.Inject;
import jakarta.inject.Named;

public class AccountingSaveObserver {
  private final ChartAccountService accountService;
  private final AccountingRoleDefinitionService roleService;
  private final AccountingSetupEntryService setupService;
  private final AccountingRoleDefinitionSeedService seedService;
  private final AccountingPeriodService periodService;
  private final JournalEntryService journalEntryService;
  private final JournalLineService journalLineService;

  @Inject
  public AccountingSaveObserver(
      ChartAccountService accountService,
      AccountingRoleDefinitionService roleService,
      AccountingSetupEntryService setupService,
      AccountingRoleDefinitionSeedService seedService,
      AccountingPeriodService periodService,
      JournalEntryService journalEntryService,
      JournalLineService journalLineService) {
    this.accountService = accountService;
    this.roleService = roleService;
    this.setupService = setupService;
    this.seedService = seedService;
    this.periodService = periodService;
    this.journalEntryService = journalEntryService;
    this.journalLineService = journalLineService;
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
  public void period(
      @Observes @Named(RequestEvent.SAVE) @EntityType(AccountingPeriod.class) PreRequest event) {
    process(event, AccountingPeriod.class, periodService::validate);
  }
  public void journalEntry(
      @Observes @Named(RequestEvent.SAVE) @EntityType(JournalEntry.class) PreRequest event) {
    process(event, JournalEntry.class, journalEntryService::validate);
  }
  public void journalLine(
      @Observes @Named(RequestEvent.SAVE) @EntityType(JournalLine.class) PreRequest event) {
    process(event, JournalLine.class, journalLineService::validate);
  }
}
