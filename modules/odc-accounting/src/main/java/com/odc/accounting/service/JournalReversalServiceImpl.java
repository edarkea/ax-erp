package com.odc.accounting.service;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.db.JPA;
import com.axelor.i18n.I18n;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import com.odc.accounting.db.*;
import com.odc.accounting.db.repo.JournalEntryRepository;
import com.odc.organization.service.OrganizationAccessService;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class JournalReversalServiceImpl implements JournalReversalService {
  private final JournalEntryRepository repository;
  private final JournalEntryService entryService;
  private final JournalLineService lineService;
  private final AccountingPeriodService periodService;
  private final AccountingPostingService postingService;
  private final JournalEntryBalanceService balanceService;
  private final OrganizationAccessService accessService;

  @Inject
  public JournalReversalServiceImpl(
      JournalEntryRepository repository, JournalEntryService entryService,
      JournalLineService lineService, AccountingPeriodService periodService,
      AccountingPostingService postingService, JournalEntryBalanceService balanceService,
      OrganizationAccessService accessService) {
    this.repository = repository; this.entryService = entryService; this.lineService = lineService;
    this.periodService = periodService; this.postingService = postingService;
    this.balanceService = balanceService; this.accessService = accessService;
  }

  @Override @Transactional
  public JournalReversalResult reverse(
      JournalEntry requested, LocalDate reversalDate, String reason) {
    JournalEntry original = lockEntry(requested);
    String normalized = normalizeReason(reason);
    validateForReversal(original, reversalDate, normalized);
    User user = AuthUtils.getUser();
    AccountingPeriod period = periodService.requireOpenPeriod(original.getCompany(), reversalDate);
    JournalEntry reversal = new JournalEntry();
    reversal.setCompany(original.getCompany()); reversal.setBranch(original.getBranch());
    reversal.setAccountingPeriod(period); reversal.setAccountingDate(reversalDate);
    reversal.setDocumentDate(reversalDate); reversal.setCurrency(original.getCurrency());
    reversal.setExchangeRate(original.getExchangeRate()); reversal.setParty(original.getParty());
    reversal.setEntryType("ADJUSTMENT"); reversal.setStatus("DRAFT");
    reversal.setReference(original.getEntryNumber());
    reversal.setDescription("Reversal of entry " + original.getEntryNumber() + ": " + original.getDescription());
    reversal.setSourceModule(original.getSourceModule()); reversal.setSourceModel(original.getSourceModel());
    reversal.setSourceId(original.getSourceId()); reversal.setSourceReference(original.getSourceReference());
    reversal.setReversalOf(original); reversal.setReversalReason(normalized);
    reversal = entryService.save(reversal);
    for (JournalLine source : lineService.findActiveLines(original)) {
      JournalLine target = new JournalLine();
      target.setJournalEntry(reversal); target.setSequence(source.getSequence());
      target.setAccount(source.getAccount()); target.setAccountingRoleDefinition(source.getAccountingRoleDefinition());
      target.setAccountingSetupEntry(source.getAccountingSetupEntry()); target.setParty(source.getParty());
      target.setDescription(source.getDescription()); target.setReference(source.getReference());
      target.setDueDate(source.getDueDate()); target.setDebit(source.getCredit()); target.setCredit(source.getDebit());
      lineService.save(target);
    }
    AccountingPostingResult posted = postingService.post(reversal);
    original.setStatus("REVERSED"); original.setReversedAt(LocalDateTime.now()); original.setReversedBy(user);
    persist(original);
    return new JournalReversalResult(original, posted.journalEntry(), original.getEntryNumber(),
        posted.entryNumber(), reversalDate, normalized, original.getReversedAt(), user);
  }

  @Override
  public void validateForReversal(JournalEntry entry, LocalDate date, String reason) {
    if (entry == null || entry.getId() == null) throw error("Persisted journal entry is required.");
    if (Boolean.TRUE.equals(entry.getArchived()) || !"POSTED".equals(entry.getStatus()))
      throw error("Only a posted journal entry can be reversed.");
    if (entry.getReversalOf() != null) throw error("A reversal entry cannot be reversed.");
    if (entry.getEntryNumber() == null) throw error("Posted journal entry has no number.");
    if (date == null) throw error("Reversal date is required.");
    normalizeReason(reason);
    if (entry.getAccountingDate() != null && date.isBefore(entry.getAccountingDate()))
      throw error("Reversal date cannot be before the original accounting date.");
    User user = AuthUtils.getUser();
    if (user == null) throw error("Authenticated user is required.");
    accessService.requireCompanyAccess(user, entry.getCompany());
    if (entry.getBranch() != null) accessService.requireBranchAccess(user, entry.getBranch());
    periodService.requireOpenPeriod(entry.getCompany(), date);
    balanceService.requireBalanced(entry);
    if (findReversal(entry).isPresent()) throw error("Journal entry already has a reversal.");
  }

  @Override
  public Optional<JournalEntry> findReversal(JournalEntry original) {
    if (original == null || original.getId() == null) return Optional.empty();
    List<JournalEntry> values = findReversals(original);
    if (values.size() > 1) throw error("Multiple reversals exist for the journal entry.");
    return values.stream().findFirst();
  }
  protected List<JournalEntry> findReversals(JournalEntry original) {
    return repository.all().filter("self.reversalOf = :original AND self.archived = false")
        .bind("original", original).fetch(0, 2);
  }
  protected JournalEntry lockEntry(JournalEntry value) {
    if (value == null || value.getId() == null) throw error("Persisted journal entry is required.");
    JournalEntry entry = JPA.em().find(JournalEntry.class, value.getId(), LockModeType.PESSIMISTIC_WRITE);
    if (entry == null) throw error("Journal entry does not exist.");
    return entry;
  }
  protected JournalEntry persist(JournalEntry entry) { return repository.save(entry); }
  private String normalizeReason(String reason) {
    if (reason == null || reason.trim().isEmpty()) throw error("Reversal reason is required.");
    return reason.trim();
  }
  private IllegalArgumentException error(String message) {
    return new IllegalArgumentException(I18n.get(message));
  }
}
