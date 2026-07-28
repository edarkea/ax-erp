package com.odc.sales.accounting.service;

import static org.junit.jupiter.api.Assertions.*;

import com.odc.accounting.db.JournalEntry;
import com.odc.accounting.db.JournalLine;
import com.odc.accounting.service.*;
import com.odc.organization.db.Company;
import com.odc.sales.db.SalesInvoice;
import com.odc.sales.service.SalesInvoiceCancellationService;
import com.odc.sales.service.SalesInvoiceCancellationServiceImpl;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;

class SalesInvoiceAccountingServiceTest {
  @Test
  void postsOnceAndReturnsSameEntryOnSecondRequest() {
    Company company = new Company(); company.setId(1L);
    SalesInvoice invoice = new SalesInvoice(); invoice.setId(2L); invoice.setCompany(company);
    invoice.setDocumentNo("001-001-000000001"); invoice.setStatus("CONFIRMED");
    JournalEntry draft = new JournalEntry(); draft.setCompany(company); draft.setStatus("DRAFT");
    draft.setSourceModel(SalesAccountingConstants.SALES_INVOICE_SOURCE_MODEL);
    draft.setSourceRecordId(invoice.getId()); draft.setSourceDocumentNo(invoice.getDocumentNo());
    JournalLine first = new JournalLine(); JournalLine second = new JournalLine();
    SalesInvoicePostingPlan plan = new SalesInvoicePostingPlan(
        invoice, null, draft, List.of(first, second), null, null, null, null, null, 2);
    EntryStub entries = new EntryStub();
    LineStub lines = new LineStub();
    PostingStub posting = new PostingStub();
    var service = new TestService(invoice, value -> plan, entries, lines, posting,
        new ReversalStub(), new CancellationStub());

    SalesInvoiceAccountingResult initial = service.postInvoice(invoice);
    SalesInvoiceAccountingResult retry = service.postInvoice(invoice);

    assertFalse(initial.alreadyPosted());
    assertTrue(retry.alreadyPosted());
    assertSame(initial.journalEntry(), retry.journalEntry());
    assertEquals(1, entries.saved);
    assertEquals(2, lines.saved);
    assertEquals(1, posting.calls);
  }

  @Test
  void rejectsExistingDraftAndMismatchedDocumentNumber() {
    Company company = new Company(); company.setId(1L);
    SalesInvoice invoice = new SalesInvoice(); invoice.setId(2L); invoice.setCompany(company);
    invoice.setDocumentNo("INV-1"); invoice.setStatus("CONFIRMED");
    EntryStub entries = new EntryStub();
    entries.value = new JournalEntry(); entries.value.setCompany(company);
    entries.value.setSourceRecordId(2L); entries.value.setSourceDocumentNo("INV-1");
    entries.value.setStatus("DRAFT");
    var service = new TestService(invoice, value -> null, entries, new LineStub(),
        new PostingStub(), new ReversalStub(), new CancellationStub());
    assertThrows(IllegalArgumentException.class, () -> service.postInvoice(invoice));
    entries.value.setStatus("POSTED"); entries.value.setSourceDocumentNo("OTHER");
    assertThrows(IllegalArgumentException.class, () -> service.postInvoice(invoice));
  }

  private static class TestService extends SalesInvoiceAccountingServiceImpl {
    private final SalesInvoice locked;
    TestService(
        SalesInvoice locked, SalesInvoicePostingMapper mapper, JournalEntryService entries,
        JournalLineService lines, AccountingPostingService posting,
        JournalReversalService reversals, SalesInvoiceCancellationServiceImpl cancellations) {
      super(mapper, entries, lines, posting, reversals, cancellations); this.locked = locked;
    }
    @Override protected SalesInvoice lockInvoice(SalesInvoice value) { return locked; }
    @Override protected void requirePermission(String permissionName) {}
  }

  private static class EntryStub implements JournalEntryService {
    JournalEntry value; int saved;
    public JournalEntry save(JournalEntry entry) {
      saved++; entry.setId(10L); value = entry; return entry;
    }
    public Optional<JournalEntry> findBySource(Company c, String m, Long id) {
      return Optional.ofNullable(value);
    }
    public void validate(JournalEntry e) {}
    public JournalEntry resolvePeriod(JournalEntry e) { return e; }
    public JournalEntry cancel(JournalEntry e, String r) { return e; }
    public void archive(JournalEntry e) {}
    public JournalEntry restore(JournalEntry e) { return e; }
    public void requireEditable(JournalEntry e) {}
    public void requireUsable(JournalEntry e) {}
  }

  private static class LineStub implements JournalLineService {
    int saved;
    public JournalLine save(JournalLine line) { saved++; return line; }
    public void validate(JournalLine line) {}
    public void validateForPosting(JournalLine line) {}
    public void archive(JournalLine line) {}
    public JournalLine restore(JournalLine line) { return line; }
    public void requireEditable(JournalLine line) {}
    public List<JournalLine> findActiveLines(JournalEntry entry) { return List.of(); }
    public List<JournalLine> lockActiveLines(JournalEntry entry) { return List.of(); }
  }

  private static class PostingStub implements AccountingPostingService {
    int calls;
    public AccountingPostingResult post(JournalEntry entry) {
      calls++; entry.setStatus("POSTED"); entry.setEntryNumber("2026-000000001");
      return new AccountingPostingResult(entry, entry.getEntryNumber(), 2026, 1L,
          null, null, null, null, 2);
    }
    public void validateForPosting(JournalEntry entry) {}
    public boolean isReadyForPosting(JournalEntry entry) { return true; }
  }

  private static class ReversalStub implements JournalReversalService {
    public JournalReversalResult reverse(JournalEntry e, LocalDate d, String r) { return null; }
    public void validateForReversal(JournalEntry e, LocalDate d, String r) {}
    public Optional<JournalEntry> findReversal(JournalEntry e) { return Optional.empty(); }
  }

  private static class CancellationStub extends SalesInvoiceCancellationServiceImpl {
    CancellationStub() { super(null); }
    @Override public SalesInvoice cancel(SalesInvoice invoice, String reason) { return invoice; }
  }
}
