package com.odc.accounting.service;

import static com.odc.accounting.service.AccountingTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import com.odc.accounting.db.AccountingPeriod;
import com.odc.organization.db.Company;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AccountingPeriodServiceImplTest {
  private Company company;
  private ActiveStub active;
  private AccessStub access;
  private Store store;
  private TestService service;

  @BeforeEach
  void setUp() {
    company = company(1); active = new ActiveStub(company); access = new AccessStub();
    store = new Store(); service = new TestService(active, access, store);
  }

  @Test
  void shouldCreateDraftAssignCompanyAndNormalize() {
    AccountingPeriod value = period(null, company(2), " 2026-01 ", " January ", "2026-01-01", "2026-01-31");
    value.setStatus("OPEN");
    AccountingPeriod saved = service.save(value);
    assertSame(company, saved.getCompany());
    assertEquals("2026-01", saved.getCode());
    assertEquals("January", saved.getName());
    assertEquals("DRAFT", saved.getStatus());
  }

  @Test
  void shouldRejectMissingCompanyAccessAndManipulation() {
    active.company = null;
    assertThrows(IllegalArgumentException.class, () -> service.save(
        period(null, company, "A", "A", "2026-01-01", "2026-01-31")));
    active.company = company; access.allowed = false;
    assertThrows(IllegalArgumentException.class, () -> service.save(
        period(null, company, "A", "A", "2026-01-01", "2026-01-31")));
    access.allowed = true;
    AccountingPeriod value = period(1L, company(2), "A", "A", "2026-01-01", "2026-01-31");
    store.snapshots.put(1L, copy(value, company));
    assertThrows(IllegalArgumentException.class, () -> service.validate(value));
  }

  @Test
  void shouldRejectRequiredFieldsInvertedRangeAndNegativeSequence() {
    AccountingPeriod value = period(null, company, " ", "A", "2026-01-01", "2026-01-31");
    assertThrows(IllegalArgumentException.class, () -> service.save(value));
    value.setCode("A"); value.setName(" ");
    assertThrows(IllegalArgumentException.class, () -> service.save(value));
    value.setName("A"); value.setStartDate(null);
    assertThrows(IllegalArgumentException.class, () -> service.save(value));
    value.setStartDate(LocalDate.of(2026, 2, 1)); value.setEndDate(LocalDate.of(2026, 1, 1));
    assertThrows(IllegalArgumentException.class, () -> service.save(value));
    value.setEndDate(value.getStartDate()); value.setSequence(-1);
    assertThrows(IllegalArgumentException.class, () -> service.save(value));
    value.setSequence(0);
    assertDoesNotThrow(() -> service.save(value));
  }

  @Test
  void shouldEnforceCodeUniquenessPerCompanyAndRestoreConflict() {
    service.save(period(null, company, "A", "A", "2026-01-01", "2026-01-10"));
    assertThrows(IllegalArgumentException.class, () -> service.save(
        period(null, company, "A", "Other", "2026-02-01", "2026-02-10")));
    TestService otherService = new TestService(new ActiveStub(company(2)), new AccessStub(), store);
    assertDoesNotThrow(() -> otherService.save(
        period(null, company(2), "A", "A", "2026-01-01", "2026-01-10")));
  }

  @Test
  void shouldAllowConsecutiveAndRejectEveryInclusiveOverlapShape() {
    service.save(period(null, company, "A", "A", "2026-01-01", "2026-01-31"));
    assertDoesNotThrow(() -> service.save(
        period(null, company, "B", "B", "2026-02-01", "2026-02-28")));
    for (String[] range : List.of(
        new String[]{"2026-01-01", "2026-01-31"},
        new String[]{"2026-01-01", "2026-01-15"},
        new String[]{"2026-01-15", "2026-01-31"},
        new String[]{"2026-01-10", "2026-01-20"},
        new String[]{"2025-12-01", "2026-02-01"},
        new String[]{"2026-01-31", "2026-02-10"})) {
      assertThrows(IllegalArgumentException.class, () -> service.save(
          period(null, company, "X" + range[0], "X", range[0], range[1])));
    }
  }

  @Test
  void shouldIgnoreArchivedOverlapAndOverlapAllStatuses() {
    AccountingPeriod archived = period(1L, company, "OLD", "Old", "2026-01-01", "2026-01-31");
    archived.setArchived(true); store.values.add(archived);
    assertDoesNotThrow(() -> service.save(
        period(null, company, "NEW", "New", "2026-01-01", "2026-01-31")));
    for (String status : List.of("DRAFT", "OPEN", "CLOSED")) {
      store.clear();
      AccountingPeriod existing = period(1L, company, status, status, "2026-01-01", "2026-01-31");
      existing.setStatus(status); store.values.add(existing);
      assertThrows(IllegalArgumentException.class, () -> service.save(
          period(null, company, "X", "X", "2026-01-15", "2026-02-15")));
    }
  }

  @Test
  void shouldApplyStateMachineAndReopenPermission() {
    AccountingPeriod value = service.save(
        period(null, company, "A", "A", "2026-01-01", "2026-01-31"));
    assertEquals("OPEN", service.open(value).getStatus());
    assertThrows(IllegalArgumentException.class, () -> service.open(value));
    assertEquals("CLOSED", service.close(value).getStatus());
    assertThrows(IllegalArgumentException.class, () -> service.close(value));
    service.reopenAllowed = false;
    assertThrows(IllegalArgumentException.class, () -> service.reopen(value));
    service.reopenAllowed = true;
    assertEquals("OPEN", service.reopen(value).getStatus());
    assertThrows(IllegalArgumentException.class, () -> service.reopen(value));
  }

  @Test
  void shouldRejectDirectStatusAndStructuralChangesAfterOpening() {
    AccountingPeriod value = service.save(
        period(null, company, "A", "A", "2026-01-01", "2026-01-31"));
    AccountingPeriod snapshot = copy(value, company); store.snapshots.put(value.getId(), snapshot);
    value.setStatus("OPEN");
    assertThrows(IllegalArgumentException.class, () -> service.validate(value));
    value.setStatus("DRAFT"); service.open(value);
    store.snapshots.put(value.getId(), copy(value, company));
    value.setStartDate(LocalDate.of(2026, 1, 2));
    assertThrows(IllegalArgumentException.class, () -> service.validate(value));
  }

  @Test
  void shouldArchiveOnlyDraftAndRestoreAsDraftWithValidation() {
    AccountingPeriod draft = service.save(
        period(null, company, "A", "A", "2026-01-01", "2026-01-31"));
    service.archive(draft); assertTrue(draft.getArchived());
    AccountingPeriod open = service.save(
        period(null, company, "B", "B", "2026-02-01", "2026-02-28"));
    service.open(open);
    assertThrows(IllegalArgumentException.class, () -> service.archive(open));
    service.close(open);
    assertThrows(IllegalArgumentException.class, () -> service.archive(open));
    assertEquals("DRAFT", service.restore(draft).getStatus());
  }

  @Test
  void shouldResolveInclusiveDatesStatusesAndIntegrity() {
    AccountingPeriod value = service.save(
        period(null, company, "A", "A", "2026-01-01", "2026-01-31"));
    assertSame(value, service.requirePeriod(company, LocalDate.of(2026, 1, 1)));
    assertSame(value, service.requirePeriod(company, LocalDate.of(2026, 1, 31)));
    assertTrue(service.findPeriod(company, LocalDate.of(2026, 2, 1)).isEmpty());
    assertThrows(IllegalArgumentException.class,
        () -> service.requireOpenPeriod(company, LocalDate.of(2026, 1, 15)));
    service.open(value);
    assertSame(value, service.requireOpenPeriod(LocalDate.of(2026, 1, 15)));
    service.close(value);
    assertThrows(IllegalArgumentException.class,
        () -> service.requireOpenPeriod(company, LocalDate.of(2026, 1, 15)));
    store.values.add(copy(value, company));
    assertThrows(IllegalArgumentException.class,
        () -> service.findPeriod(company, LocalDate.of(2026, 1, 15)));
  }

  @Test
  void shouldSerializeConcurrentOverlapsAndAllowConcurrentConsecutivePeriods() throws Exception {
    assertEquals(1, concurrentSaves(
        period(null, company, "A", "A", "2026-01-01", "2026-01-31"),
        period(null, company, "B", "B", "2026-01-15", "2026-02-15")));
    store.clear();
    assertEquals(2, concurrentSaves(
        period(null, company, "A", "A", "2026-01-01", "2026-01-31"),
        period(null, company, "B", "B", "2026-02-01", "2026-02-28")));
  }

  private int concurrentSaves(AccountingPeriod first, AccountingPeriod second) throws Exception {
    ExecutorService executor = Executors.newFixedThreadPool(2);
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<Boolean>> results = List.of(
        executor.submit(() -> saveAfterLatch(first, ready, start)),
        executor.submit(() -> saveAfterLatch(second, ready, start)));
    ready.await(); start.countDown();
    int successes = 0;
    for (Future<Boolean> result : results) if (result.get()) successes++;
    executor.shutdownNow();
    return successes;
  }
  private boolean saveAfterLatch(
      AccountingPeriod value, CountDownLatch ready, CountDownLatch start) throws Exception {
    ready.countDown(); start.await();
    try { service.save(value); return true; }
    catch (IllegalArgumentException exception) { return false; }
  }

  private static AccountingPeriod period(
      Long id, Company company, String code, String name, String start, String end) {
    AccountingPeriod value = new AccountingPeriod();
    value.setId(id); value.setCompany(company); value.setCode(code); value.setName(name);
    value.setStartDate(LocalDate.parse(start)); value.setEndDate(LocalDate.parse(end));
    value.setStatus("DRAFT"); value.setSequence(0); value.setArchived(false);
    return value;
  }
  private static AccountingPeriod copy(AccountingPeriod source, Company company) {
    AccountingPeriod value = period(source.getId(), company, source.getCode(), source.getName(),
        source.getStartDate().toString(), source.getEndDate().toString());
    value.setStatus(source.getStatus()); value.setNotes(source.getNotes());
    value.setSequence(source.getSequence()); value.setArchived(source.getArchived());
    return value;
  }

  private static class Store {
    final Object lock = new Object();
    final List<AccountingPeriod> values = new ArrayList<>();
    final Map<Long, AccountingPeriod> snapshots = new ConcurrentHashMap<>();
    final AtomicLong ids = new AtomicLong();
    void clear() { values.clear(); snapshots.clear(); ids.set(0); }
  }
  private static class TestService extends AccountingPeriodServiceImpl {
    final Store store;
    boolean reopenAllowed = true;
    TestService(ActiveStub active, AccessStub access, Store store) {
      super(null, active, access); this.store = store;
    }
    @Override protected <T> T withCompanyLock(Company company, Supplier<T> work) {
      synchronized (store.lock) { return work.get(); }
    }
    @Override protected AccountingPeriod lockRequired(AccountingPeriod value) { return value; }
    @Override protected AccountingPeriod findPersisted(Long id) {
      return id == null ? null : store.snapshots.get(id);
    }
    @Override protected AccountingPeriod findDuplicateCode(AccountingPeriod value) {
      return store.values.stream().filter(other -> !Boolean.TRUE.equals(other.getArchived())
          && other.getCompany().getId().equals(value.getCompany().getId())
          && other.getCode().equals(value.getCode())
          && (value.getId() == null || !value.getId().equals(other.getId()))).findFirst().orElse(null);
    }
    @Override protected List<AccountingPeriod> findOverlappingPeriods(AccountingPeriod value) {
      return store.values.stream().filter(other -> !Boolean.TRUE.equals(other.getArchived())
          && other.getCompany().getId().equals(value.getCompany().getId())
          && (value.getId() == null || !value.getId().equals(other.getId()))
          && !other.getStartDate().isAfter(value.getEndDate())
          && !other.getEndDate().isBefore(value.getStartDate())).toList();
    }
    @Override protected List<AccountingPeriod> findApplicablePeriods(Company company, LocalDate date) {
      return store.values.stream().filter(value -> !Boolean.TRUE.equals(value.getArchived())
          && value.getCompany().getId().equals(company.getId())
          && !date.isBefore(value.getStartDate()) && !date.isAfter(value.getEndDate())).toList();
    }
    @Override protected AccountingPeriod persist(AccountingPeriod value) {
      if (value.getId() == null) { value.setId(store.ids.incrementAndGet()); store.values.add(value); }
      store.snapshots.put(value.getId(), copy(value, value.getCompany()));
      return value;
    }
    @Override protected boolean canReopen() { return reopenAllowed; }
  }
}
