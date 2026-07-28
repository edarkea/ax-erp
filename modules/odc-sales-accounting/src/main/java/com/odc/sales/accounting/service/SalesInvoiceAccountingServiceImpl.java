package com.odc.sales.accounting.service;

import com.axelor.auth.AuthUtils;
import com.axelor.db.JPA;
import com.axelor.i18n.I18n;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import com.odc.accounting.db.JournalEntry;
import com.odc.accounting.service.*;
import com.odc.sales.db.SalesInvoice;
import com.odc.sales.service.SalesInvoiceCancellationService;
import com.odc.sales.service.SalesInvoiceCancellationServiceImpl;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

public class SalesInvoiceAccountingServiceImpl implements SalesInvoiceAccountingService {
  private final SalesInvoicePostingMapper mapper;
  private final JournalEntryService entries;
  private final JournalLineService lines;
  private final AccountingPostingService posting;
  private final JournalReversalService reversals;
  private final SalesInvoiceCancellationServiceImpl cancellations;

  @Inject
  public SalesInvoiceAccountingServiceImpl(
      SalesInvoicePostingMapper mapper, JournalEntryService entries, JournalLineService lines,
      AccountingPostingService posting, JournalReversalService reversals,
      SalesInvoiceCancellationServiceImpl cancellations) {
    this.mapper = mapper; this.entries = entries; this.lines = lines; this.posting = posting;
    this.reversals = reversals; this.cancellations = cancellations;
  }

  @Override @Transactional
  public SalesInvoiceAccountingResult postInvoice(SalesInvoice requested) {
    requirePermission("odc.sales-accounting.invoice.post");
    SalesInvoice invoice = lockInvoice(requested);
    Optional<JournalEntry> existing = findPosting(invoice);
    if (existing.isPresent()) {
      JournalEntry entry = validateExisting(invoice, existing.get());
      if ("POSTED".equals(entry.getStatus()))
        return new SalesInvoiceAccountingResult(invoice, entry, true);
      if ("REVERSED".equals(entry.getStatus()))
        throw error("Invoice posting was reversed.");
      if ("DRAFT".equals(entry.getStatus()))
        throw error("A draft journal entry already exists for this invoice.");
      throw error("An inconsistent journal entry already exists for this invoice.");
    }
    SalesInvoicePostingPlan plan = mapper.map(invoice);
    JournalEntry entry = entries.save(plan.journalEntry());
    for (var line : plan.journalLines()) {
      line.setJournalEntry(entry);
      lines.save(line);
    }
    JournalEntry posted = posting.post(entry).journalEntry();
    return new SalesInvoiceAccountingResult(invoice, posted, false);
  }

  @Override
  public Optional<JournalEntry> findPosting(SalesInvoice invoice) {
    if (invoice == null || invoice.getId() == null || invoice.getCompany() == null)
      return Optional.empty();
    return entries.findBySource(
        invoice.getCompany(), SalesAccountingConstants.SALES_INVOICE_SOURCE_MODEL, invoice.getId());
  }

  @Override
  public JournalEntry requirePosting(SalesInvoice invoice) {
    requirePermission("odc.sales-accounting.invoice.view-entry");
    return requirePostingInternal(invoice);
  }

  private JournalEntry requirePostingInternal(SalesInvoice invoice) {
    return findPosting(invoice)
        .map(entry -> validateExisting(invoice, entry))
        .orElseThrow(() -> error("Invoice does not have a journal entry yet."));
  }

  @Override
  public boolean isPosted(SalesInvoice invoice) {
    return findPosting(invoice).map(entry -> "POSTED".equals(entry.getStatus())).orElse(false);
  }

  @Override @Transactional
  public JournalReversalResult reverseInvoicePosting(
      SalesInvoice requested, LocalDate reversalDate, String reason) {
    requirePermission("odc.sales-accounting.invoice.reverse");
    SalesInvoice invoice = lockInvoice(requested);
    if (!"CONFIRMED".equals(invoice.getStatus())) throw error("Only a confirmed invoice can be reversed.");
    JournalEntry entry = requirePostingInternal(invoice);
    if (!"POSTED".equals(entry.getStatus())) throw error("Invoice posting is not posted.");
    return reversals.reverse(entry, reversalDate, reason);
  }

  @Override @Transactional
  public SalesInvoice cancelWithReversal(
      SalesInvoice requested, LocalDate reversalDate, String reason) {
    requirePermission("odc.sales-accounting.invoice.cancel-with-reversal");
    SalesInvoice invoice = lockInvoice(requested);
    Optional<JournalEntry> entry = findPosting(invoice);
    if (entry.isPresent() && "POSTED".equals(entry.get().getStatus()))
      reversals.reverse(entry.get(), reversalDate, reason);
    else if (entry.isPresent() && !"REVERSED".equals(entry.get().getStatus()))
      throw error("Invoice accounting is inconsistent and cannot be cancelled.");
    return cancellations.cancel(invoice, reason);
  }

  protected SalesInvoice lockInvoice(SalesInvoice value) {
    if (value == null || value.getId() == null) throw error("Persisted invoice is required.");
    SalesInvoice invoice = JPA.em().find(SalesInvoice.class, value.getId(), LockModeType.PESSIMISTIC_WRITE);
    if (invoice == null) throw error("Invoice does not exist.");
    return invoice;
  }

  protected void requirePermission(String permissionName) {
    var user = AuthUtils.getUser();
    if (user == null) throw error("Authenticated user is required.");
    if (AuthUtils.isAdmin(user)) return;
    boolean direct = user.getRoles() != null && user.getRoles().stream()
        .filter(Objects::nonNull).flatMap(role -> role.getPermissions().stream())
        .anyMatch(permission -> permissionName.equals(permission.getName()));
    boolean group = user.getGroup() != null && user.getGroup().getRoles() != null
        && user.getGroup().getRoles().stream().filter(Objects::nonNull)
            .flatMap(role -> role.getPermissions().stream())
            .anyMatch(permission -> permissionName.equals(permission.getName()));
    if (!direct && !group) throw error("You do not have permission for this accounting action.");
  }

  private JournalEntry validateExisting(SalesInvoice invoice, JournalEntry entry) {
    if (!Objects.equals(entry.getSourceDocumentNo(), invoice.getDocumentNo()))
      throw error("Journal entry source document number does not match the invoice.");
    if (!Objects.equals(entry.getSourceRecordId(), invoice.getId())
        || !Objects.equals(entry.getCompany().getId(), invoice.getCompany().getId()))
      throw error("Journal entry source identity is inconsistent.");
    return entry;
  }

  private IllegalArgumentException error(String message) {
    return new IllegalArgumentException(I18n.get(message));
  }
}
