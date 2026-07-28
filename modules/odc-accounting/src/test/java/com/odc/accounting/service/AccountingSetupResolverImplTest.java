package com.odc.accounting.service;

import static com.odc.accounting.service.AccountingTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import com.odc.accounting.db.AccountingRoleDefinition;
import com.odc.accounting.db.AccountingSetupEntry;
import com.odc.accounting.db.ChartAccount;
import com.odc.organization.db.Branch;
import com.odc.organization.db.Company;
import com.odc.reference.db.Currency;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AccountingSetupResolverImplTest {
  private Company company;
  private Branch branch;
  private Currency currency;
  private AccountingRoleDefinition role;
  private ChartAccount account;
  private AccessStub access;
  private TestResolver resolver;

  @BeforeEach
  void setUp() {
    company = company(1); branch = branch(1, company); currency = currency(1);
    role = role(1, "ACCOUNT_RECEIVABLE", "SALES", "DEBIT");
    account = account(1, company, "1.1", true);
    access = new AccessStub();
    resolver = new TestResolver(new ActiveStub(company), access);
  }

  @Test
  void shouldResolveGlobalAndReturnCompleteDto() {
    AccountingSetupEntry global = setup(1, company, role, account);
    resolver.values.add(global);
    AccountingSetupResolution result =
        resolver.requireAccount(company, branch, currency, "SALES", "SALES_INVOICE", role);
    assertSame(global, result.setupEntry()); assertSame(account, result.account());
    assertSame(role, result.roleDefinition()); assertEquals(0, result.specificityScore());
  }

  @Test
  void shouldPreferDocumentThenBranchThenCurrencySpecificity() {
    resolver.values.add(setup(1, company, role, account));
    AccountingSetupEntry document = setup(2, company, role, account(2, company, "DOC", true));
    document.setDocumentType("SALES_INVOICE"); resolver.values.add(document);
    AccountingSetupEntry branchEntry = setup(3, company, role, account(3, company, "BR", true));
    branchEntry.setDocumentType("SALES_INVOICE"); branchEntry.setBranch(branch);
    resolver.values.add(branchEntry);
    AccountingSetupEntry exact = setup(4, company, role, account(4, company, "ALL", true));
    exact.setDocumentType("SALES_INVOICE"); exact.setBranch(branch); exact.setCurrency(currency);
    exact.setPriority(999); resolver.values.add(exact);
    assertSame(exact, resolver.requireAccount(
        company, branch, currency, "SALES", "SALES_INVOICE", role).setupEntry());
  }

  @Test
  void shouldLetSpecificityWinBeforeNumericPriority() {
    AccountingSetupEntry global = setup(1, company, role, account); global.setPriority(1);
    AccountingSetupEntry specific = setup(2, company, role, account(2, company, "S", true));
    specific.setBranch(branch); specific.setPriority(1000);
    resolver.values.add(global); resolver.values.add(specific);
    assertSame(specific, resolver.requireAccount(
        company, branch, currency, "SALES", null, role).setupEntry());
  }

  @Test
  void shouldUseLowerPriorityForEqualSpecificityAndRejectFinalTie() {
    AccountingSetupEntry first = setup(1, company, role, account); first.setPriority(20);
    AccountingSetupEntry second = setup(2, company, role, account(2, company, "B", true));
    second.setPriority(10); resolver.values.add(first); resolver.values.add(second);
    assertSame(second, resolver.requireAccount(
        company, null, null, "SALES", null, role).setupEntry());
    first.setPriority(10);
    assertThrows(IllegalArgumentException.class, () -> resolver.requireAccount(
        company, null, null, "SALES", null, role));
  }

  @Test
  void shouldIgnoreArchivedSetupAndInvalidAccount() {
    AccountingSetupEntry archived = setup(1, company, role, account);
    archived.setArchived(true);
    AccountingSetupEntry invalid = setup(2, company, role, account(2, company, "B", true));
    invalid.getAccount().setArchived(true);
    resolver.values.add(archived); resolver.values.add(invalid);
    assertTrue(resolver.findAccount(company, branch, currency, "SALES", null, role).isEmpty());
  }

  @Test
  void shouldReturnEmptyOrThrowWhenMissing() {
    assertTrue(resolver.findAccount(company, null, null, "SALES", null, role).isEmpty());
    assertThrows(IllegalArgumentException.class,
        () -> resolver.requireAccount(company, null, null, "SALES", null, role));
  }

  @Test
  void shouldRejectArchivedRoleOtherCompanyBranchAndNoAccess() {
    role.setArchived(true);
    assertThrows(IllegalArgumentException.class,
        () -> resolver.findAccount(company, branch, currency, "SALES", null, role));
    role.setArchived(false);
    assertThrows(IllegalArgumentException.class,
        () -> resolver.findAccount(company, branch(2, company(2)), currency, "SALES", null, role));
    access.allowed = false;
    assertThrows(IllegalArgumentException.class,
        () -> resolver.findAccount(company, branch, currency, "SALES", null, role));
  }

  @Test
  void shouldIsolateCompanies() {
    Company other = company(2);
    resolver.values.add(setup(1, other, role, account(2, other, "OTHER", true)));
    assertTrue(resolver.findAccount(company, null, null, "SALES", null, role).isEmpty());
  }

  private static class TestResolver extends AccountingSetupResolverImpl {
    final List<AccountingSetupEntry> values = new ArrayList<>();
    TestResolver(ActiveStub active, AccessStub access) {
      super(null, active, access, new RoleStub(),
          new AccountingConfigurationValidationServiceImpl());
    }
    @Override protected List<AccountingSetupEntry> findCandidates(
        Company company, String group, AccountingRoleDefinition role) {
      return values.stream().filter(value -> value.getCompany() != null
          && value.getCompany().getId().equals(company.getId())
          && value.getDocumentGroup().equals(group)
          && value.getAccountingRoleDefinition().getId().equals(role.getId())
          && Boolean.TRUE.equals(value.getActive())
          && !Boolean.TRUE.equals(value.getArchived())).toList();
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
