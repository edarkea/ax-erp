package com.odc.accounting.service;

import static com.odc.accounting.service.AccountingTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import com.odc.accounting.db.AccountingPeriod;
import com.odc.accounting.db.JournalEntry;
import com.odc.organization.db.Company;
import com.odc.party.db.Party;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JournalEntryServiceImplTest {
  private Company company;
  private AccountingPeriod period;
  private PeriodStub periods;
  private TestService service;

  @BeforeEach
  void setUp() {
    company = company(1);
    period = new AccountingPeriod();
    period.setId(10L); period.setCompany(company); period.setStatus("OPEN");
    period.setStartDate(LocalDate.of(2026, 1, 1)); period.setEndDate(LocalDate.of(2026, 12, 31));
    period.setArchived(false);
    periods = new PeriodStub(period);
    service = new TestService(new ActiveStub(company), new AccessStub(), periods);
  }

  @Test
  void createsDraftForActiveCompanyWithoutNumberAndNormalizes() {
    JournalEntry value = valid();
    value.setCompany(company(2)); value.setStatus("POSTED"); value.setEntryNumber("1");
    value.setDescription(" Entry "); value.setSourceModel(" Invoice ");
    JournalEntry saved = service.save(value);
    assertSame(company, saved.getCompany());
    assertEquals("DRAFT", saved.getStatus());
    assertNull(saved.getEntryNumber());
    assertEquals("Entry", saved.getDescription());
    assertEquals("Invoice", saved.getSourceModel());
    assertSame(period, saved.getAccountingPeriod());
  }

  @Test
  void rejectsInvalidRequiredValuesAndExchangeRate() {
    JournalEntry missingDescription = valid(); missingDescription.setDescription(" ");
    assertThrows(IllegalArgumentException.class, () -> service.save(missingDescription));
    JournalEntry zeroRate = valid(); zeroRate.setExchangeRate(BigDecimal.ZERO);
    assertThrows(IllegalArgumentException.class, () -> service.save(zeroRate));
    JournalEntry missingDate = valid(); missingDate.setAccountingDate(null);
    assertThrows(IllegalArgumentException.class, () -> service.save(missingDate));
  }

  @Test
  void validatesBranchCurrencyPartyAndCompany() {
    JournalEntry otherBranch = valid();
    otherBranch.setBranch(branch(1, company(2)));
    assertThrows(IllegalArgumentException.class, () -> service.save(otherBranch));
    JournalEntry archivedCurrency = valid(); archivedCurrency.getCurrency().setArchived(true);
    assertThrows(IllegalArgumentException.class, () -> service.save(archivedCurrency));
    JournalEntry otherParty = valid(); otherParty.setParty(party(1, company(2)));
    assertThrows(IllegalArgumentException.class, () -> service.save(otherParty));
  }

  @Test
  void rejectsClosedOrMismatchedPeriod() {
    JournalEntry mismatch = valid();
    AccountingPeriod other = new AccountingPeriod(); other.setId(11L); other.setCompany(company);
    mismatch.setAccountingPeriod(other);
    assertThrows(IllegalArgumentException.class, () -> service.save(mismatch));
    JournalEntry closed = valid(); period.setStatus("CLOSED");
    assertThrows(IllegalArgumentException.class, () -> service.save(closed));
  }

  @Test
  void rejectsDirectStateNumberAndCompanyChangesAndImmutableStatuses() {
    JournalEntry value = service.save(valid());
    JournalEntry snapshot = copy(value); service.persisted = snapshot;
    value.setStatus("POSTED");
    assertThrows(IllegalArgumentException.class, () -> service.validate(value));
    value.setStatus("DRAFT"); value.setEntryNumber("X");
    assertThrows(IllegalArgumentException.class, () -> service.validate(value));
    value.setEntryNumber(null); value.setCompany(company(2));
    assertThrows(IllegalArgumentException.class, () -> service.validate(value));
    value.setCompany(company); snapshot.setStatus("POSTED");
    assertThrows(IllegalArgumentException.class, () -> service.validate(value));
  }

  @Test
  void cancelsDraftWithReasonAndLocksFurtherChanges() {
    JournalEntry value = service.save(valid());
    assertThrows(IllegalArgumentException.class, () -> service.cancel(value, " "));
    assertEquals("CANCELLED", service.cancel(value, " Error ").getStatus());
    assertEquals("Error", value.getCancelReason());
    assertNotNull(value.getCancelledAt());
    assertThrows(IllegalArgumentException.class, () -> service.cancel(value, "Again"));
  }

  @Test
  void archivesAndRestoresOnlyDraftWithFullValidation() {
    JournalEntry value = service.save(valid());
    service.archive(value); assertTrue(value.getArchived());
    assertEquals("DRAFT", service.restore(value).getStatus());
    value.setStatus("CANCELLED");
    assertThrows(IllegalArgumentException.class, () -> service.archive(value));
    value.setArchived(true);
    assertThrows(IllegalArgumentException.class, () -> service.restore(value));
  }

  @Test
  void findsAtMostOneSourcePerCompany() {
    JournalEntry value = service.save(valid());
    value.setSourceModel("Invoice"); value.setSourceId(5L);
    assertSame(value, service.findBySource(company, " Invoice ", 5L).orElseThrow());
    assertTrue(service.findBySource(company, "Invoice", 6L).isEmpty());
    service.values.add(copy(value));
    assertThrows(IllegalArgumentException.class,
        () -> service.findBySource(company, "Invoice", 5L));
  }

  private JournalEntry valid() {
    JournalEntry value = new JournalEntry();
    value.setCompany(company); value.setEntryType("GENERAL");
    value.setAccountingDate(LocalDate.of(2026, 7, 28));
    value.setCurrency(currency(1)); value.setExchangeRate(BigDecimal.ONE);
    value.setDescription("Entry"); value.setArchived(false); value.setStatus("DRAFT");
    return value;
  }

  private static Party party(long id, Company company) {
    Party value = new Party(); value.setId(id); value.setCompany(company);
    value.setActive(true); value.setArchived(false); return value;
  }

  private static JournalEntry copy(JournalEntry source) {
    JournalEntry value = new JournalEntry();
    value.setId(source.getId()); value.setCompany(source.getCompany()); value.setBranch(source.getBranch());
    value.setAccountingPeriod(source.getAccountingPeriod()); value.setEntryNumber(source.getEntryNumber());
    value.setEntryType(source.getEntryType()); value.setStatus(source.getStatus());
    value.setAccountingDate(source.getAccountingDate()); value.setDocumentDate(source.getDocumentDate());
    value.setCurrency(source.getCurrency()); value.setExchangeRate(source.getExchangeRate());
    value.setParty(source.getParty()); value.setDescription(source.getDescription());
    value.setSourceModel(source.getSourceModel()); value.setSourceId(source.getSourceId());
    value.setArchived(source.getArchived()); return value;
  }

  private static class TestService extends JournalEntryServiceImpl {
    final List<JournalEntry> values = new ArrayList<>();
    final AtomicLong ids = new AtomicLong();
    JournalEntry persisted;
    TestService(ActiveStub active, AccessStub access, AccountingPeriodService periods) {
      super(null, active, access, periods);
    }
    @Override protected JournalEntry persist(JournalEntry value) {
      if (value.getId() == null) { value.setId(ids.incrementAndGet()); values.add(value); }
      return value;
    }
    @Override protected JournalEntry lockRequired(JournalEntry value) { return value; }
    @Override protected JournalEntry findPersisted(Long id) { return persisted; }
    @Override protected List<JournalEntry> findSourceEntries(
        Company company, String model, Long sourceId) {
      return values.stream().filter(value -> value.getCompany().getId().equals(company.getId())
          && model.equals(value.getSourceModel()) && sourceId.equals(value.getSourceId())).toList();
    }
  }

  private static class PeriodStub implements AccountingPeriodService {
    AccountingPeriod period;
    PeriodStub(AccountingPeriod period) { this.period = period; }
    public AccountingPeriod save(AccountingPeriod value) { return value; }
    public void validate(AccountingPeriod value) {}
    public AccountingPeriod open(AccountingPeriod value) { return value; }
    public AccountingPeriod close(AccountingPeriod value) { return value; }
    public AccountingPeriod reopen(AccountingPeriod value) { return value; }
    public void archive(AccountingPeriod value) {}
    public AccountingPeriod restore(AccountingPeriod value) { return value; }
    public Optional<AccountingPeriod> findPeriod(Company company, LocalDate date) {
      return Optional.ofNullable(period);
    }
    public AccountingPeriod requirePeriod(Company company, LocalDate date) {
      if (period == null) throw new IllegalArgumentException();
      return period;
    }
    public AccountingPeriod requireOpenPeriod(Company company, LocalDate date) { return period; }
    public AccountingPeriod requireOpenPeriod(LocalDate date) { return period; }
    public boolean isDateOpen(Company company, LocalDate date) { return "OPEN".equals(period.getStatus()); }
  }
}
