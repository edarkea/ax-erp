package com.odc.accounting.service;

import static com.odc.accounting.service.AccountingTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import com.axelor.auth.db.User;
import com.odc.accounting.db.JournalEntry;
import com.odc.accounting.db.JournalLine;
import com.odc.organization.db.Company;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AccountingPostingServiceImplTest {
  private Company company;
  private JournalEntry entry;
  private BalanceStub balance;
  private TestService service;

  @BeforeEach
  void setUp() {
    company = company(1);
    entry = new JournalEntry(); entry.setId(1L); entry.setCompany(company);
    entry.setStatus("DRAFT"); entry.setArchived(false);
    entry.setAccountingDate(LocalDate.of(2026, 1, 15));
    balance = new BalanceStub();
    service = new TestService(new EntryStub(), new LineStub(), balance, new AccessStub());
  }

  @Test
  void postsAndReturnsDefinitiveNumberAndTotals() {
    AccountingPostingResult result = service.post(entry);
    assertEquals("POSTED", entry.getStatus());
    assertEquals(2026, entry.getPostingYear());
    assertEquals(1L, entry.getPostingSequence());
    assertEquals("2026-000000001", entry.getEntryNumber());
    assertNotNull(entry.getPostedAt()); assertSame(service.user, entry.getPostedBy());
    assertEquals(new BigDecimal("100"), result.totalDebit());
    assertEquals(2, result.lineCount());
  }

  @Test
  void incrementsPerCompanyAndYearAndUsesNineDigitPadding() {
    service.maximum = 41;
    assertEquals("2026-000000042", service.post(entry).entryNumber());
    entry = fresh(2, company, LocalDate.of(2027, 1, 1));
    service.maximum = 0;
    assertEquals("2027-000000001", service.post(entry).entryNumber());
    Company other = company(2);
    entry = fresh(3, other, LocalDate.of(2026, 1, 1));
    assertEquals("2026-000000001", service.post(entry).entryNumber());
  }

  @Test
  void rejectsInvalidStatesArchiveUnsavedAndExistingPostingData() {
    for (String status : List.of("POSTED", "REVERSED", "CANCELLED")) {
      entry.setStatus(status);
      assertThrows(IllegalArgumentException.class, () -> service.validateForPosting(entry));
    }
    entry.setStatus("DRAFT"); entry.setArchived(true);
    assertThrows(IllegalArgumentException.class, () -> service.validateForPosting(entry));
    entry.setArchived(false); entry.setId(null);
    assertThrows(IllegalArgumentException.class, () -> service.validateForPosting(entry));
    entry.setId(1L); entry.setPostingSequence(1L);
    assertThrows(IllegalArgumentException.class, () -> service.validateForPosting(entry));
  }

  @Test
  void readinessDoesNotMutateOrConsumeNumber() {
    assertTrue(service.isReadyForPosting(entry));
    assertNull(entry.getEntryNumber());
    balance.fail = true;
    assertFalse(service.isReadyForPosting(entry));
    assertNull(entry.getEntryNumber());
  }

  private JournalEntry fresh(long id, Company company, LocalDate date) {
    JournalEntry value = new JournalEntry(); value.setId(id); value.setCompany(company);
    value.setStatus("DRAFT"); value.setArchived(false); value.setAccountingDate(date); return value;
  }

  private static class TestService extends AccountingPostingServiceImpl {
    final User user = new User();
    long maximum;
    TestService(JournalEntryService entries, JournalLineService lines,
        JournalEntryBalanceService balance, AccessStub access) {
      super(null, entries, lines, balance, access); user.setId(1L);
    }
    @Override protected JournalEntry lockEntry(JournalEntry value) { return value; }
    @Override protected void lockCompany(Company value) {}
    @Override protected long findMaxPostingSequence(Company value, int year) { return maximum; }
    @Override protected JournalEntry persist(JournalEntry value) { return value; }
    @Override protected User requireUser() { return user; }
  }

  private static class BalanceStub implements JournalEntryBalanceService {
    boolean fail;
    public JournalEntryTotals calculateTotals(JournalEntry value) {
      return new JournalEntryTotals(new BigDecimal("100"), new BigDecimal("100"),
          BigDecimal.ZERO, 2, true, true, null, 2);
    }
    public boolean isBalanced(JournalEntry value) { return !fail; }
    public void requireBalanced(JournalEntry value) { if (fail) throw new IllegalArgumentException(); }
    public void requireReadyForPosting(JournalEntry value) {
      if (fail) throw new IllegalArgumentException();
    }
  }

  private static class EntryStub implements JournalEntryService {
    public JournalEntry save(JournalEntry value) { return value; }
    public void validate(JournalEntry value) {}
    public JournalEntry resolvePeriod(JournalEntry value) { return value; }
    public JournalEntry cancel(JournalEntry value, String reason) { return value; }
    public void archive(JournalEntry value) {}
    public JournalEntry restore(JournalEntry value) { return value; }
    public void requireEditable(JournalEntry value) {
      if (!"DRAFT".equals(value.getStatus())) throw new IllegalArgumentException();
    }
    public void requireUsable(JournalEntry value) {}
    public Optional<JournalEntry> findBySource(Company c, String m, Long id) { return Optional.empty(); }
  }

  private static class LineStub implements JournalLineService {
    public JournalLine save(JournalLine value) { return value; }
    public void validate(JournalLine value) {}
    public void validateForPosting(JournalLine value) {}
    public void archive(JournalLine value) {}
    public JournalLine restore(JournalLine value) { return value; }
    public void requireEditable(JournalLine value) {}
    public List<JournalLine> findActiveLines(JournalEntry value) { return List.of(); }
    public List<JournalLine> lockActiveLines(JournalEntry value) { return List.of(); }
  }
}
