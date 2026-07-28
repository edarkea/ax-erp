package com.odc.accounting.service;

import static com.odc.accounting.service.AccountingTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import com.odc.accounting.db.ChartAccount;
import com.odc.organization.db.Company;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChartAccountServiceImplTest {
  private Company company;
  private ActiveStub active;
  private AccessStub access;
  private TestService service;

  @BeforeEach
  void setUp() {
    company = company(1); active = new ActiveStub(company); access = new AccessStub();
    service = new TestService(active, access);
  }

  @Test
  void shouldCreateNormalizeAssignCompanyAndDefaultBalance() {
    ChartAccount value = account(0, company, " ignored ", false);
    value.setId(null); value.setCompany(company(2)); value.setCode("  cash ");
    value.setName(" Cash "); value.setNormalBalance(null);
    ChartAccount saved = service.save(value);
    assertSame(company, saved.getCompany());
    assertEquals("CASH", saved.getCode());
    assertEquals("Cash", saved.getName());
    assertEquals("DEBIT", saved.getNormalBalance());
  }

  @Test
  void shouldRejectMissingActiveCompanyManipulationAndAccess() {
    active.company = null;
    assertThrows(IllegalArgumentException.class,
        () -> service.validate(account(1, company, "A", false)));
    active.company = company;
    assertThrows(IllegalArgumentException.class,
        () -> service.validate(account(1, company(2), "A", false)));
    access.allowed = false;
    assertThrows(IllegalArgumentException.class,
        () -> service.validate(account(1, company, "A", false)));
  }

  @Test
  void shouldRejectDuplicateOnlyInsideSameCompany() {
    service.duplicate = account(2, company, "A", false);
    assertThrows(IllegalArgumentException.class,
        () -> service.validate(account(1, company, "A", false)));
    service.duplicate = null;
    assertDoesNotThrow(() -> service.validate(account(1, company, "A", false)));
  }

  @Test
  void shouldValidateParentCompanyTypeAndPostingState() {
    ChartAccount child = account(2, company, "1.1", false);
    ChartAccount parent = account(1, company(2), "1", false);
    child.setParent(parent);
    assertThrows(IllegalArgumentException.class, () -> service.validateHierarchy(child));
    parent.setCompany(company); parent.setAccountType("INCOME");
    assertThrows(IllegalArgumentException.class, () -> service.validateHierarchy(child));
    parent.setAccountType("ASSET"); parent.setIsPosting(true);
    assertThrows(IllegalArgumentException.class, () -> service.validateHierarchy(child));
  }

  @Test
  void shouldRejectSelfTwoLevelAndThreeLevelCycles() {
    ChartAccount a = account(1, company, "A", false);
    a.setParent(a);
    assertThrows(IllegalArgumentException.class, () -> service.validateHierarchy(a));
    ChartAccount b = account(2, company, "B", false);
    a.setParent(b); b.setParent(a);
    assertThrows(IllegalArgumentException.class, () -> service.validateHierarchy(a));
    ChartAccount c = account(3, company, "C", false);
    a.setParent(b); b.setParent(c); c.setParent(a);
    assertThrows(IllegalArgumentException.class, () -> service.validateHierarchy(a));
  }

  @Test
  void shouldAllowGroupingChildrenAndRejectPostingWithChildren() {
    ChartAccount value = account(1, company, "A", false);
    value.setChildren(new ArrayList<>());
    value.getChildren().add(account(2, company, "B", true));
    assertDoesNotThrow(() -> service.validate(value));
    value.setIsPosting(true);
    service.children = true;
    assertThrows(IllegalArgumentException.class, () -> service.validate(value));
  }

  @Test
  void shouldRejectNegativeSequenceAndCompanyChange() {
    ChartAccount value = account(1, company, "A", false); value.setSequence(-1);
    assertThrows(IllegalArgumentException.class, () -> service.validate(value));
    value.setSequence(0); service.persisted = account(1, company(2), "A", false);
    assertThrows(IllegalArgumentException.class, () -> service.validate(value));
  }

  @Test
  void shouldArchiveOnlyUnusedLeafAndRestoreWithValidation() {
    ChartAccount value = account(1, company, "A", true);
    service.archive(value);
    assertTrue(value.getArchived()); assertFalse(value.getActive());
    service.children = true; value.setArchived(false); value.setActive(true);
    assertThrows(IllegalArgumentException.class, () -> service.archive(value));
    service.children = false; service.configured = true;
    assertThrows(IllegalArgumentException.class, () -> service.archive(value));
    service.configured = false; service.duplicate = account(2, company, "A", true);
    value.setArchived(true); value.setActive(false);
    assertThrows(IllegalArgumentException.class, () -> service.restore(value));
  }

  @Test
  void shouldRequirePostingAccount() {
    ChartAccount value = account(1, company, "A", true);
    assertDoesNotThrow(() -> service.requirePostingAccount(value));
    value.setIsPosting(false);
    assertThrows(IllegalArgumentException.class, () -> service.requirePostingAccount(value));
  }

  private static class TestService extends ChartAccountServiceImpl {
    ChartAccount duplicate;
    ChartAccount persisted;
    boolean children;
    boolean configured;
    TestService(ActiveStub active, AccessStub access) { super(null, null, active, access); }
    @Override protected ChartAccount findDuplicate(ChartAccount value) { return duplicate; }
    @Override protected boolean hasActiveChildren(ChartAccount value) {
      return children || superChildren(value);
    }
    private boolean superChildren(ChartAccount value) {
      return value.getId() == null && value.getChildren() != null
          && value.getChildren().stream().anyMatch(child -> Boolean.TRUE.equals(child.getActive())
              && !Boolean.TRUE.equals(child.getArchived()));
    }
    @Override protected boolean hasActiveSetupEntries(ChartAccount value) { return configured; }
    @Override protected ChartAccount findPersisted(Long id) { return persisted; }
    @Override protected ChartAccount persist(ChartAccount value) { return value; }
  }
}
