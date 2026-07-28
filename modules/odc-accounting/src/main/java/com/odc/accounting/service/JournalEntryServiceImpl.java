package com.odc.accounting.service;

import com.axelor.auth.AuthUtils;
import com.axelor.db.JPA;
import com.axelor.i18n.I18n;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import com.odc.accounting.db.AccountingPeriod;
import com.odc.accounting.db.JournalEntry;
import com.odc.accounting.db.repo.JournalEntryRepository;
import com.odc.organization.db.Branch;
import com.odc.organization.db.Company;
import com.odc.organization.service.ActiveOrganizationService;
import com.odc.organization.service.OrganizationAccessService;
import com.odc.party.db.Party;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class JournalEntryServiceImpl implements JournalEntryService {
  private static final List<String> TYPES =
      List.of("GENERAL", "ADJUSTMENT", "OPENING", "CLOSING", "SALES", "PURCHASE", "CASH", "BANK");
  private static final List<String> STATUSES = List.of("DRAFT", "POSTED", "REVERSED", "CANCELLED");
  private final JournalEntryRepository repository;
  private final ActiveOrganizationService activeOrganizationService;
  private final OrganizationAccessService accessService;
  private final AccountingPeriodService periodService;

  @Inject
  public JournalEntryServiceImpl(
      JournalEntryRepository repository,
      ActiveOrganizationService activeOrganizationService,
      OrganizationAccessService accessService,
      AccountingPeriodService periodService) {
    this.repository = repository;
    this.activeOrganizationService = activeOrganizationService;
    this.accessService = accessService;
    this.periodService = periodService;
  }

  @Override @Transactional
  public JournalEntry save(JournalEntry entry) {
    prepareNew(entry);
    validateCore(entry, false);
    return persist(entry);
  }

  @Override @Transactional
  public void validate(JournalEntry entry) {
    prepareNew(entry);
    validateCore(entry, false);
  }

  @Override
  public JournalEntry resolvePeriod(JournalEntry entry) {
    if (entry == null) throw error("Journal entry is required.");
    Company company = entry.getCompany() == null
        ? activeOrganizationService.requireActiveCompany() : entry.getCompany();
    entry.setCompany(company);
    if (entry.getAccountingDate() == null) {
      entry.setAccountingPeriod(null);
      return entry;
    }
    entry.setAccountingPeriod(periodService.requirePeriod(company, entry.getAccountingDate()));
    return entry;
  }

  @Override @Transactional
  public JournalEntry cancel(JournalEntry requested, String reason) {
    JournalEntry entry = lockRequired(requested);
    requireEditable(entry);
    String normalized = required(reason, "Cancellation reason is required.");
    validateCore(entry, true);
    entry.setCancelReason(normalized);
    entry.setCancelledAt(LocalDateTime.now());
    entry.setStatus("CANCELLED");
    return persist(entry);
  }

  @Override @Transactional
  public void archive(JournalEntry requested) {
    JournalEntry entry = lockRequired(requested);
    requireEditable(entry);
    entry.setArchived(true);
    persist(entry);
  }

  @Override @Transactional
  public JournalEntry restore(JournalEntry requested) {
    JournalEntry entry = lockRequired(requested);
    if (!Boolean.TRUE.equals(entry.getArchived()) || !"DRAFT".equals(entry.getStatus()))
      throw error("Only an archived draft journal entry can be restored.");
    entry.setArchived(false);
    validateCore(entry, true);
    return persist(entry);
  }

  @Override
  public void requireEditable(JournalEntry entry) {
    requireUsable(entry);
    if (!"DRAFT".equals(entry.getStatus())) throw error("Only draft journal entries can be modified.");
  }

  @Override
  public void requireUsable(JournalEntry entry) {
    if (entry == null) throw error("Journal entry is required.");
    requireCompany(entry.getCompany());
    if (Boolean.TRUE.equals(entry.getArchived())) throw error("Journal entry is archived.");
  }

  @Override
  public Optional<JournalEntry> findBySource(Company company, String sourceModel, Long sourceRecordId) {
    requireCompany(company);
    String model = required(sourceModel, "Source model is required.");
    if (sourceRecordId == null || sourceRecordId < 0) throw error("Source identifier must not be negative.");
    List<JournalEntry> entries = findSourceEntries(company, model, sourceRecordId);
    if (entries.size() > 1) throw error("Multiple journal entries exist for the same source.");
    return entries.stream().findFirst();
  }

  protected void validateCore(JournalEntry entry, boolean businessAction) {
    if (entry == null) throw error("Journal entry is required.");
    requireCompany(entry.getCompany());
    defaults(entry);
    if (!TYPES.contains(entry.getEntryType())) throw error("Journal entry type is invalid.");
    if (!STATUSES.contains(entry.getStatus())) throw error("Journal entry status is invalid.");
    if (entry.getAccountingDate() == null) throw error("Accounting date is required.");
    if (entry.getCurrency() == null || Boolean.TRUE.equals(entry.getCurrency().getArchived()))
      throw error("Currency must be active.");
    if (entry.getExchangeRate() == null || entry.getExchangeRate().compareTo(BigDecimal.ZERO) <= 0)
      throw error("Exchange rate must be greater than zero.");
    entry.setDescription(required(entry.getDescription(), "Description is required."));
    entry.setReference(optional(entry.getReference(), 255, "Reference is too long."));
    entry.setSourceModule(optional(entry.getSourceModule(), 50, "Source module is too long."));
    entry.setSourceModel(optional(entry.getSourceModel(), 255, "Source model is too long."));
    entry.setSourceDocumentNo(optional(entry.getSourceDocumentNo(), 255, "Source reference is too long."));
    if (entry.getSourceRecordId() != null && entry.getSourceRecordId() < 0)
      throw error("Source identifier must not be negative.");
    validateBranch(entry);
    validateParty(entry);
    validatePeriod(entry);
    JournalEntry persisted = findPersisted(entry.getId());
    if (persisted != null) {
      if (!same(persisted.getCompany(), entry.getCompany()))
        throw error("Journal entry company cannot be changed.");
      if (!businessAction && !Objects.equals(persisted.getStatus(), entry.getStatus()))
        throw error("Journal entry status can only change through business actions.");
      if (!businessAction && !Objects.equals(persisted.getArchived(), entry.getArchived()))
        throw error("Journal entry archive state can only change through business actions.");
      if (!businessAction && !Objects.equals(persisted.getEntryNumber(), entry.getEntryNumber()))
        throw error("Journal entry number cannot be changed manually.");
      if (!businessAction
          && (!Objects.equals(persisted.getPostingYear(), entry.getPostingYear())
              || !Objects.equals(persisted.getPostingSequence(), entry.getPostingSequence())
              || !Objects.equals(persisted.getPostedAt(), entry.getPostedAt())
              || !same(persisted.getPostedBy(), entry.getPostedBy())
              || !Objects.equals(persisted.getReversedAt(), entry.getReversedAt())
              || !same(persisted.getReversedBy(), entry.getReversedBy())
              || !same(persisted.getReversalOf(), entry.getReversalOf())))
        throw error("Posting and reversal data can only change through business actions.");
      if (!"DRAFT".equals(persisted.getStatus()))
        throw error("Posted, reversed or cancelled journal entries are immutable.");
    }
    if ("DRAFT".equals(entry.getStatus()) && entry.getEntryNumber() != null)
      throw error("Draft journal entry cannot have an entry number.");
  }

  private void validateBranch(JournalEntry entry) {
    Branch branch = entry.getBranch();
    if (branch == null) return;
    if (!same(branch.getCompany(), entry.getCompany()) || Boolean.TRUE.equals(branch.getArchived())
        || !Boolean.TRUE.equals(branch.getActive())) throw error("Branch must be active and belong to the journal entry company.");
    accessService.requireBranchAccess(AuthUtils.getUser(), branch);
  }

  private void validateParty(JournalEntry entry) {
    Party party = entry.getParty();
    if (party == null) return;
    if (!same(party.getCompany(), entry.getCompany()) || Boolean.TRUE.equals(party.getArchived())
        || !Boolean.TRUE.equals(party.getActive())) throw error("Party must be active and belong to the journal entry company.");
  }

  private void validatePeriod(JournalEntry entry) {
    AccountingPeriod resolved = periodService.requirePeriod(entry.getCompany(), entry.getAccountingDate());
    if (entry.getAccountingPeriod() == null) entry.setAccountingPeriod(resolved);
    if (!same(entry.getAccountingPeriod(), resolved))
      throw error("Accounting period does not contain the accounting date.");
    AccountingPeriod period = entry.getAccountingPeriod();
    if (!same(period.getCompany(), entry.getCompany()) || Boolean.TRUE.equals(period.getArchived()))
      throw error("Accounting period must be active and belong to the journal entry company.");
    if ("CLOSED".equals(period.getStatus()))
      throw error("Accounting period is closed.");
    if (!List.of("DRAFT", "OPEN").contains(period.getStatus()))
      throw error("Accounting period status is invalid.");
  }

  private void prepareNew(JournalEntry entry) {
    if (entry == null) throw error("Journal entry is required.");
    if (entry.getId() == null) {
      entry.setCompany(activeOrganizationService.requireActiveCompany());
      entry.setStatus("DRAFT");
      entry.setEntryNumber(null);
      entry.setPostingYear(null);
      entry.setPostingSequence(null);
      entry.setPostedAt(null);
      entry.setPostedBy(null);
      entry.setReversedAt(null);
      entry.setReversedBy(null);
      entry.setArchived(false);
    }
  }

  private void defaults(JournalEntry entry) {
    if (entry.getArchived() == null) entry.setArchived(false);
    if (entry.getStatus() == null) entry.setStatus("DRAFT");
    if (entry.getExchangeRate() == null) entry.setExchangeRate(BigDecimal.ONE);
  }

  private void requireCompany(Company company) {
    if (company == null || Boolean.TRUE.equals(company.getArchived())
        || !Boolean.TRUE.equals(company.getActive())) throw error("Company must be active.");
    accessService.requireCompanyAccess(AuthUtils.getUser(), company);
    if (!same(company, activeOrganizationService.requireActiveCompany()))
      throw error("Journal entry must belong to the active company.");
  }

  protected JournalEntry findPersisted(Long id) { return id == null ? null : repository.find(id); }
  protected JournalEntry persist(JournalEntry entry) { return repository.save(entry); }
  protected JournalEntry lockRequired(JournalEntry value) {
    if (value == null || value.getId() == null) throw error("Persisted journal entry is required.");
    JournalEntry locked = JPA.em().find(JournalEntry.class, value.getId(), LockModeType.PESSIMISTIC_WRITE);
    if (locked == null) throw error("Journal entry does not exist.");
    return locked;
  }
  protected List<JournalEntry> findSourceEntries(Company company, String model, Long sourceRecordId) {
    return repository.all()
        .filter("self.company = :company AND self.sourceModel = :model "
            + "AND self.sourceRecordId = :sourceRecordId AND self.reversalOf IS NULL "
            + "AND self.archived = false")
        .bind("company", company).bind("model", model)
        .bind("sourceRecordId", sourceRecordId).fetch(0, 2);
  }
  private String required(String value, String message) {
    if (value == null || value.trim().isEmpty()) throw error(message);
    return value.trim();
  }
  private String optional(String value, int max, String message) {
    if (value == null || value.trim().isEmpty()) return null;
    String result = value.trim();
    if (result.length() > max) throw error(message);
    return result;
  }
  private boolean same(com.axelor.db.Model left, com.axelor.db.Model right) {
    return left == right || left != null && right != null && left.getId() != null
        && Objects.equals(left.getId(), right.getId());
  }
  private IllegalArgumentException error(String message) {
    return new IllegalArgumentException(I18n.get(message));
  }
}
