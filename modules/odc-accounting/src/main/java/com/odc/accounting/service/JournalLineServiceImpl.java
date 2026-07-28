package com.odc.accounting.service;

import com.axelor.db.JPA;
import com.axelor.i18n.I18n;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import com.odc.accounting.db.*;
import com.odc.accounting.db.repo.JournalLineRepository;
import com.odc.organization.db.Company;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public class JournalLineServiceImpl implements JournalLineService {
  private final JournalLineRepository repository;
  private final JournalEntryService entryService;
  private final ChartAccountService accountService;

  @Inject
  public JournalLineServiceImpl(
      JournalLineRepository repository, JournalEntryService entryService,
      ChartAccountService accountService) {
    this.repository = repository; this.entryService = entryService; this.accountService = accountService;
  }

  @Override @Transactional
  public JournalLine save(JournalLine line) {
    validate(line);
    return persist(line);
  }

  @Override
  public void validate(JournalLine line) {
    if (line == null) throw error("Journal line is required.");
    requireEditable(line);
    validateContent(line);
  }

  @Override
  public void validateForPosting(JournalLine line) {
    if (line == null || line.getJournalEntry() == null) throw error("Journal line is required.");
    validateContent(line);
  }

  private void validateContent(JournalLine line) {
    if (line.getSequence() == null) line.setSequence(nextSequence(line.getJournalEntry()));
    if (line.getSequence() <= 0) throw error("Journal line sequence must be positive.");
    defaults(line);
    BigDecimal debit = line.getDebit(), credit = line.getCredit();
    if (debit.signum() < 0) throw error("Debit cannot be negative.");
    if (credit.signum() < 0) throw error("Credit cannot be negative.");
    if ((debit.signum() > 0) == (credit.signum() > 0))
      throw error("Journal line must contain either debit or credit.");
    Company company = line.getJournalEntry().getCompany();
    accountService.requirePostingAccount(line.getAccount());
    if (!same(line.getAccount().getCompany(), company))
      throw error("Account belongs to another company.");
    validateRole(line);
    validateParty(line, company);
    validateSetup(line, company);
    if ("DRAFT".equals(line.getJournalEntry().getStatus()) && findDuplicateSequence(line) != null)
      throw error("Journal line sequence already exists.");
    line.setDescription(trim(line.getDescription()));
    line.setReference(trim(line.getReference()));
  }

  @Override @Transactional
  public void archive(JournalLine value) {
    JournalLine line = lockRequired(value);
    requireEditable(line);
    line.setArchived(true); persist(line);
  }

  @Override @Transactional
  public JournalLine restore(JournalLine value) {
    JournalLine line = lockRequired(value);
    if (!Boolean.TRUE.equals(line.getArchived())) throw error("Journal line is not archived.");
    line.setArchived(false); validate(line); return persist(line);
  }

  @Override
  public void requireEditable(JournalLine line) {
    if (line == null || line.getJournalEntry() == null)
      throw error("Journal entry is required.");
    entryService.requireEditable(line.getJournalEntry());
  }

  @Override
  public List<JournalLine> findActiveLines(JournalEntry entry) {
    if (entry == null) throw error("Journal entry is required.");
    return queryActiveLines(entry, false);
  }

  @Override
  public List<JournalLine> lockActiveLines(JournalEntry entry) {
    return queryActiveLines(entry, true);
  }

  protected List<JournalLine> queryActiveLines(JournalEntry entry, boolean lock) {
    var query = repository.all()
        .filter("self.journalEntry = :entry AND self.archived = false")
        .bind("entry", entry).order("sequence");
    List<JournalLine> lines = query.fetch();
    if (lock) lines.forEach(line -> JPA.em().lock(line, LockModeType.PESSIMISTIC_WRITE));
    return lines;
  }
  protected JournalLine findDuplicateSequence(JournalLine line) {
    String filter = "self.journalEntry = :entry AND self.sequence = :sequence AND self.archived = false";
    var query = repository.all().filter(filter)
        .bind("entry", line.getJournalEntry()).bind("sequence", line.getSequence());
    if (line.getId() != null) query = repository.all().filter(filter + " AND self.id != :id")
        .bind("entry", line.getJournalEntry()).bind("sequence", line.getSequence()).bind("id", line.getId());
    return query.fetchOne();
  }
  protected int nextSequence(JournalEntry entry) {
    return findActiveLines(entry).stream().map(JournalLine::getSequence)
        .filter(Objects::nonNull).max(Integer::compareTo).orElse(0) + 10;
  }
  protected JournalLine persist(JournalLine line) { return repository.save(line); }
  protected JournalLine lockRequired(JournalLine value) {
    if (value == null || value.getId() == null) throw error("Persisted journal line is required.");
    JournalLine line = JPA.em().find(JournalLine.class, value.getId(), LockModeType.PESSIMISTIC_WRITE);
    if (line == null) throw error("Journal line does not exist.");
    return line;
  }
  private void validateRole(JournalLine line) {
    AccountingRoleDefinition role = line.getAccountingRoleDefinition();
    if (role == null) return;
    if (Boolean.TRUE.equals(role.getArchived()) || !Boolean.TRUE.equals(role.getActive()))
      throw error("Accounting role must be active.");
    if ("DEBIT".equals(role.getSideHint()) && line.getDebit().signum() <= 0)
      throw error("Accounting role requires a debit.");
    if ("CREDIT".equals(role.getSideHint()) && line.getCredit().signum() <= 0)
      throw error("Accounting role requires a credit.");
    if (Boolean.TRUE.equals(role.getRequiresParty()) && line.getParty() == null)
      throw error("Accounting role requires a party.");
    if (Boolean.TRUE.equals(role.getRequiresDueDate()) && line.getDueDate() == null)
      throw error("Accounting role requires a due date.");
    LocalDate base = line.getJournalEntry().getDocumentDate() == null
        ? line.getJournalEntry().getAccountingDate() : line.getJournalEntry().getDocumentDate();
    if (line.getDueDate() != null && base != null && line.getDueDate().isBefore(base))
      throw error("Due date cannot be before the journal entry date.");
  }
  private void validateParty(JournalLine line, Company company) {
    if (line.getParty() == null) return;
    if (!same(line.getParty().getCompany(), company) || Boolean.TRUE.equals(line.getParty().getArchived())
        || !Boolean.TRUE.equals(line.getParty().getActive()))
      throw error("Party must be active and belong to the journal entry company.");
  }
  private void validateSetup(JournalLine line, Company company) {
    AccountingSetupEntry setup = line.getAccountingSetupEntry();
    if (setup == null) return;
    if (!same(setup.getCompany(), company) || !same(setup.getAccount(), line.getAccount())
        || !same(setup.getAccountingRoleDefinition(), line.getAccountingRoleDefinition())
        || Boolean.TRUE.equals(setup.getArchived()) || !Boolean.TRUE.equals(setup.getActive()))
      throw error("Accounting setup is incompatible with the journal line.");
    JournalEntry entry = line.getJournalEntry();
    if (setup.getBranch() != null && !same(setup.getBranch(), entry.getBranch()))
      throw error("Accounting setup branch is incompatible.");
    if (setup.getCurrency() != null && !same(setup.getCurrency(), entry.getCurrency()))
      throw error("Accounting setup currency is incompatible.");
  }
  private void defaults(JournalLine line) {
    if (line.getArchived() == null) line.setArchived(false);
    if (line.getDebit() == null) line.setDebit(BigDecimal.ZERO);
    if (line.getCredit() == null) line.setCredit(BigDecimal.ZERO);
  }
  private String trim(String value) { return value == null || value.trim().isEmpty() ? null : value.trim(); }
  private boolean same(com.axelor.db.Model left, com.axelor.db.Model right) {
    return left == right || left != null && right != null && left.getId() != null
        && Objects.equals(left.getId(), right.getId());
  }
  private IllegalArgumentException error(String message) {
    return new IllegalArgumentException(I18n.get(message));
  }
}
