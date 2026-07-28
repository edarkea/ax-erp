package com.odc.accounting.service;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.db.JPA;
import com.axelor.i18n.I18n;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import com.odc.accounting.db.JournalEntry;
import com.odc.accounting.db.repo.JournalEntryRepository;
import com.odc.organization.db.Company;
import com.odc.organization.service.OrganizationAccessService;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;

public class AccountingPostingServiceImpl implements AccountingPostingService {
  private final JournalEntryRepository repository;
  private final JournalEntryService entryService;
  private final JournalLineService lineService;
  private final JournalEntryBalanceService balanceService;
  private final OrganizationAccessService accessService;

  @Inject
  public AccountingPostingServiceImpl(
      JournalEntryRepository repository, JournalEntryService entryService,
      JournalLineService lineService, JournalEntryBalanceService balanceService,
      OrganizationAccessService accessService) {
    this.repository = repository; this.entryService = entryService;
    this.lineService = lineService; this.balanceService = balanceService; this.accessService = accessService;
  }

  @Override @Transactional
  public AccountingPostingResult post(JournalEntry requested) {
    User user = requireUser();
    JournalEntry entry = lockEntry(requested);
    accessService.requireCompanyAccess(user, entry.getCompany());
    if (entry.getBranch() != null) accessService.requireBranchAccess(user, entry.getBranch());
    lockCompany(entry.getCompany());
    lineService.lockActiveLines(entry);
    validateForPosting(entry);
    int year = entry.getAccountingDate().getYear();
    long sequence = findMaxPostingSequence(entry.getCompany(), year) + 1;
    entry.setPostingYear(year);
    entry.setPostingSequence(sequence);
    entry.setEntryNumber(String.format("%04d-%09d", year, sequence));
    entry.setStatus("POSTED");
    entry.setPostedAt(LocalDateTime.now());
    entry.setPostedBy(user);
    JournalEntry saved = persist(entry);
    JournalEntryTotals totals = balanceService.calculateTotals(saved);
    return new AccountingPostingResult(saved, saved.getEntryNumber(), year, sequence,
        saved.getPostedAt(), user, totals.totalDebit(), totals.totalCredit(), totals.lineCount());
  }

  @Override
  public void validateForPosting(JournalEntry entry) {
    if (entry == null || entry.getId() == null) throw error("Persisted journal entry is required.");
    if (Boolean.TRUE.equals(entry.getArchived())) throw error("Journal entry is archived.");
    if (!"DRAFT".equals(entry.getStatus())) throw error("Only a draft journal entry can be posted.");
    if (entry.getEntryNumber() != null
        || entry.getPostingYear() != null && entry.getPostingYear() > 0
        || entry.getPostingSequence() != null && entry.getPostingSequence() > 0
        || entry.getPostedAt() != null || entry.getPostedBy() != null)
      throw error("Journal entry already has posting data.");
    entryService.requireEditable(entry);
    balanceService.requireReadyForPosting(entry);
  }

  @Override
  public boolean isReadyForPosting(JournalEntry entry) {
    try { validateForPosting(entry); return true; }
    catch (IllegalArgumentException exception) { return false; }
  }

  protected JournalEntry lockEntry(JournalEntry value) {
    if (value == null || value.getId() == null) throw error("Persisted journal entry is required.");
    JournalEntry entry = JPA.em().find(JournalEntry.class, value.getId(), LockModeType.PESSIMISTIC_WRITE);
    if (entry == null) throw error("Journal entry does not exist.");
    return entry;
  }
  protected void lockCompany(Company company) {
    JPA.em().find(Company.class, company.getId(), LockModeType.PESSIMISTIC_WRITE);
  }
  protected long findMaxPostingSequence(Company company, int year) {
    Long value = JPA.em().createQuery(
        "SELECT MAX(e.postingSequence) FROM JournalEntry e "
            + "WHERE e.company = :company AND e.postingYear = :year", Long.class)
        .setParameter("company", company).setParameter("year", year).getSingleResult();
    return value == null ? 0 : value;
  }
  protected JournalEntry persist(JournalEntry entry) { return repository.save(entry); }
  protected User requireUser() {
    User user = AuthUtils.getUser();
    if (user == null) throw error("Authenticated user is required.");
    return user;
  }
  private IllegalArgumentException error(String message) {
    return new IllegalArgumentException(I18n.get(message));
  }
}
