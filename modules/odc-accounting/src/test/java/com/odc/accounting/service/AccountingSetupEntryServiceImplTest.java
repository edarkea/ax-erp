package com.odc.accounting.service;

import static com.odc.accounting.service.AccountingTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import com.odc.accounting.db.AccountingRoleDefinition;
import com.odc.accounting.db.AccountingSetupEntry;
import com.odc.accounting.db.ChartAccount;
import com.odc.organization.db.Company;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AccountingSetupEntryServiceImplTest {
  private Company company;
  private AccountingRoleDefinition role;
  private ChartAccount account;
  private ActiveStub active;
  private AccessStub access;
  private TestService service;

  @BeforeEach
  void setUp() {
    company = company(1);
    role = role(1, "ACCOUNT_RECEIVABLE", "SALES", "DEBIT");
    account = account(1, company, "1.1", true);
    active = new ActiveStub(company); access = new AccessStub();
    service = new TestService(active, access);
  }

  @Test
  void shouldCreateGlobalAndAssignActiveCompany() {
    AccountingSetupEntry value = setup(0, company(2), role, account);
    value.setId(null);
    assertSame(company, service.save(value).getCompany());
    assertEquals(100, value.getPriority());
  }

  @Test
  void shouldAcceptSpecificBranchCurrencyAndDocumentType() {
    AccountingSetupEntry value = setup(1, company, role, account);
    value.setBranch(branch(1, company)); value.setCurrency(currency(1));
    value.setDocumentType("SALES_INVOICE");
    assertDoesNotThrow(() -> service.validate(value));
  }

  @Test
  void shouldRejectCompanyAccountAndBranchManipulation() {
    AccountingSetupEntry value = setup(1, company(2), role, account);
    assertThrows(IllegalArgumentException.class, () -> service.validate(value));
    value.setCompany(company); value.setAccount(account(2, company(2), "A", true));
    assertThrows(IllegalArgumentException.class, () -> service.validate(value));
    value.setAccount(account); value.setBranch(branch(1, company(2)));
    assertThrows(IllegalArgumentException.class, () -> service.validate(value));
  }

  @Test
  void shouldRejectArchivedOrGroupingRelations() {
    AccountingSetupEntry value = setup(1, company, role, account);
    account.setArchived(true);
    assertThrows(IllegalArgumentException.class, () -> service.validate(value));
    account.setArchived(false); account.setIsPosting(false);
    assertThrows(IllegalArgumentException.class, () -> service.validate(value));
    account.setIsPosting(true); role.setArchived(true);
    assertThrows(IllegalArgumentException.class, () -> service.validate(value));
    role.setArchived(false); value.setCurrency(currency(1)); value.getCurrency().setArchived(true);
    assertThrows(IllegalArgumentException.class, () -> service.validate(value));
  }

  @Test
  void shouldRejectNegativePriorityExactDuplicateAndAmbiguity() {
    AccountingSetupEntry value = setup(1, company, role, account);
    value.setPriority(-1);
    assertThrows(IllegalArgumentException.class, () -> service.validate(value));
    value.setPriority(10);
    AccountingSetupEntry duplicate = setup(2, company, role, account);
    duplicate.setPriority(20); service.peers.add(duplicate);
    assertThrows(IllegalArgumentException.class, () -> service.validate(value));
    service.peers.clear();
    AccountingSetupEntry ambiguous = setup(2, company, role, account);
    ambiguous.setPriority(10); ambiguous.setBranch(branch(2, company));
    value.setBranch(branch(1, company)); service.peers.add(ambiguous);
    assertDoesNotThrow(() -> service.validate(value));
    ambiguous.setBranch(null); value.setBranch(null);
    assertThrows(IllegalArgumentException.class, () -> service.validate(value));
  }

  @Test
  void shouldAllowDifferentSpecificityAndPreventCompanyChange() {
    AccountingSetupEntry global = setup(2, company, role, account);
    global.setPriority(100); service.peers.add(global);
    AccountingSetupEntry specific = setup(1, company, role, account);
    specific.setBranch(branch(1, company)); specific.setPriority(100);
    assertDoesNotThrow(() -> service.validate(specific));
    service.persisted = setup(1, company(2), role, account(2, company(2), "A", true));
    assertThrows(IllegalArgumentException.class, () -> service.validate(specific));
  }

  @Test
  void shouldArchiveAndRestoreWithConflict() {
    AccountingSetupEntry value = setup(1, company, role, account);
    service.archive(value);
    assertTrue(value.getArchived()); assertFalse(value.getActive());
    service.peers.add(setup(2, company, role, account));
    assertThrows(IllegalArgumentException.class, () -> service.restore(value));
  }

  @Test
  void shouldHaveNoJournalOrSalesRelations() {
    List<String> names = java.util.Arrays.stream(AccountingSetupEntry.class.getMethods())
        .map(java.lang.reflect.Method::getName).toList();
    assertFalse(names.stream().anyMatch(name ->
        name.contains("Journal") || name.contains("Sales") || name.contains("Period")));
  }

  private static class TestService extends AccountingSetupEntryServiceImpl {
    final List<AccountingSetupEntry> peers = new ArrayList<>();
    AccountingSetupEntry persisted;
    TestService(ActiveStub active, AccessStub access) {
      super(null, active, access, new AccountStub(), new RoleStub(),
          new AccountingConfigurationValidationServiceImpl());
    }
    @Override protected List<AccountingSetupEntry> activePeers(AccountingSetupEntry value) {
      return peers;
    }
    @Override protected AccountingSetupEntry findPersisted(Long id) { return persisted; }
    @Override protected AccountingSetupEntry persist(AccountingSetupEntry value) { return value; }
  }
  private static class AccountStub implements ChartAccountService {
    public ChartAccount save(ChartAccount value) { return value; }
    public void validate(ChartAccount value) {}
    public void validateHierarchy(ChartAccount value) {}
    public void archive(ChartAccount value) {}
    public ChartAccount restore(ChartAccount value) { return value; }
    public void requireUsable(ChartAccount value) {}
    public void requirePostingAccount(ChartAccount value) {
      if (value == null || Boolean.TRUE.equals(value.getArchived())
          || !Boolean.TRUE.equals(value.getActive()) || !Boolean.TRUE.equals(value.getIsPosting()))
        throw new IllegalArgumentException();
    }
  }
  private static class RoleStub implements AccountingRoleDefinitionService {
    public AccountingRoleDefinition save(AccountingRoleDefinition value) { return value; }
    public void validate(AccountingRoleDefinition value) {}
    public void archive(AccountingRoleDefinition value) {}
    public AccountingRoleDefinition restore(AccountingRoleDefinition value) { return value; }
    public void requireUsable(AccountingRoleDefinition value) {
      if (value == null || Boolean.TRUE.equals(value.getArchived())
          || !Boolean.TRUE.equals(value.getActive())) throw new IllegalArgumentException();
    }
  }
}
