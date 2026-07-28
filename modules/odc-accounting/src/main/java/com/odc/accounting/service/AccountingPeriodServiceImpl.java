package com.odc.accounting.service;

import com.axelor.auth.AuthUtils;
import com.axelor.db.JPA;
import com.axelor.i18n.I18n;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import com.odc.accounting.db.AccountingPeriod;
import com.odc.accounting.db.repo.AccountingPeriodRepository;
import com.odc.organization.db.Company;
import com.odc.organization.service.ActiveOrganizationService;
import com.odc.organization.service.OrganizationAccessService;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public class AccountingPeriodServiceImpl implements AccountingPeriodService {
  private final AccountingPeriodRepository repository;
  private final ActiveOrganizationService activeOrganizationService;
  private final OrganizationAccessService accessService;

  @Inject
  public AccountingPeriodServiceImpl(
      AccountingPeriodRepository repository,
      ActiveOrganizationService activeOrganizationService,
      OrganizationAccessService accessService) {
    this.repository = repository;
    this.activeOrganizationService = activeOrganizationService;
    this.accessService = accessService;
  }

  @Override @Transactional
  public AccountingPeriod save(AccountingPeriod period) {
    if (period == null) throw error("Accounting period is required.");
    Company active = activeOrganizationService.requireActiveCompany();
    if (period.getId() == null) {
      period.setCompany(active);
      period.setStatus("DRAFT");
      period.setArchived(false);
    }
    return withCompanyLock(period.getCompany(), () -> {
      validateCore(period, false, true);
      return persist(period);
    });
  }

  @Override @Transactional
  public void validate(AccountingPeriod period) {
    if (period == null) throw error("Accounting period is required.");
    Company active = activeOrganizationService.requireActiveCompany();
    if (period.getId() == null) {
      period.setCompany(active);
      period.setStatus("DRAFT");
      period.setArchived(false);
    }
    withCompanyLock(period.getCompany(), () -> {
      validateCore(period, false, true);
      return period;
    });
  }

  @Override @Transactional
  public AccountingPeriod open(AccountingPeriod requested) {
    AccountingPeriod period = lockRequired(requested);
    if (!"DRAFT".equals(period.getStatus())) throw error("Only a draft period can be opened.");
    if (Boolean.TRUE.equals(period.getArchived())) throw error("Archived period cannot be opened.");
    return withCompanyLock(period.getCompany(), () -> {
      validateCore(period, true, true);
      period.setStatus("OPEN");
      return persist(period);
    });
  }

  @Override @Transactional
  public AccountingPeriod close(AccountingPeriod requested) {
    AccountingPeriod period = lockRequired(requested);
    requireCompany(period.getCompany(), true);
    if (!"OPEN".equals(period.getStatus())) throw error("Only an open period can be closed.");
    period.setStatus("CLOSED");
    return persist(period);
  }

  @Override @Transactional
  public AccountingPeriod reopen(AccountingPeriod requested) {
    requireReopenPermission();
    AccountingPeriod period = lockRequired(requested);
    if (!"CLOSED".equals(period.getStatus())) throw error("Only a closed period can be reopened.");
    if (Boolean.TRUE.equals(period.getArchived())) throw error("Archived period cannot be reopened.");
    return withCompanyLock(period.getCompany(), () -> {
      validateCore(period, true, true);
      period.setStatus("OPEN");
      return persist(period);
    });
  }

  @Override @Transactional
  public void archive(AccountingPeriod requested) {
    AccountingPeriod period = lockRequired(requested);
    requireCompany(period.getCompany(), true);
    if (!"DRAFT".equals(period.getStatus()))
      throw error("Only draft periods can be archived.");
    period.setArchived(true);
    persist(period);
  }

  @Override @Transactional
  public AccountingPeriod restore(AccountingPeriod requested) {
    AccountingPeriod period = lockRequired(requested);
    if (!Boolean.TRUE.equals(period.getArchived()))
      throw error("Accounting period is not archived.");
    return withCompanyLock(period.getCompany(), () -> {
      period.setArchived(false);
      period.setStatus("DRAFT");
      validateCore(period, true, true);
      return persist(period);
    });
  }

  @Override
  public Optional<AccountingPeriod> findPeriod(Company company, LocalDate date) {
    requireCompany(company, false);
    if (date == null) throw error("Accounting date is required.");
    List<AccountingPeriod> periods = findApplicablePeriods(company, date);
    if (periods.size() > 1)
      throw error("Multiple accounting periods apply to the same date.");
    return periods.stream().findFirst();
  }

  @Override
  public AccountingPeriod requirePeriod(Company company, LocalDate date) {
    return findPeriod(company, date)
        .orElseThrow(() -> error("No accounting period exists for the selected date."));
  }

  @Override
  public AccountingPeriod requireOpenPeriod(Company company, LocalDate date) {
    AccountingPeriod period = requirePeriod(company, date);
    if (Boolean.TRUE.equals(period.getArchived()))
      throw error("Accounting period is archived.");
    if ("DRAFT".equals(period.getStatus())) throw error("Accounting period is still in draft.");
    if ("CLOSED".equals(period.getStatus())) throw error("Accounting period is closed.");
    if (!"OPEN".equals(period.getStatus())) throw error("Accounting period status is invalid.");
    return period;
  }

  @Override
  public AccountingPeriod requireOpenPeriod(LocalDate date) {
    return requireOpenPeriod(activeOrganizationService.requireActiveCompany(), date);
  }

  @Override
  public boolean isDateOpen(Company company, LocalDate date) {
    return findPeriod(company, date).filter(period -> "OPEN".equals(period.getStatus())).isPresent();
  }

  protected void validateCore(
      AccountingPeriod period, boolean allowStatusChange, boolean requireActiveContext) {
    Company active = activeOrganizationService.requireActiveCompany();
    requireCompany(period.getCompany(), requireActiveContext);
    if (requireActiveContext && !same(period.getCompany(), active))
      throw error("You do not have access to the period company.");
    defaults(period);
    period.setCode(required(period.getCode(), "Accounting period code is required.").toUpperCase());
    period.setName(required(period.getName(), "Accounting period name is required."));
    if (period.getStartDate() == null) throw error("Start date is required.");
    if (period.getEndDate() == null) throw error("End date is required.");
    if (period.getEndDate().isBefore(period.getStartDate()))
      throw error("End date cannot be before start date.");
    if (period.getSequence() < 0) throw error("Sequence cannot be negative.");
    if (!List.of("DRAFT", "OPEN", "CLOSED").contains(period.getStatus()))
      throw error("Accounting period status is invalid.");
    AccountingPeriod persisted = findPersisted(period.getId());
    if (persisted != null) {
      if (!same(persisted.getCompany(), period.getCompany()))
        throw error("Accounting period company cannot be changed.");
      if (!allowStatusChange && !Objects.equals(persisted.getStatus(), period.getStatus()))
        throw error("Accounting period status can only change through business actions.");
      if (!allowStatusChange && !Objects.equals(persisted.getArchived(), period.getArchived()))
        throw error("Accounting period archive state can only change through business actions.");
      if (!"DRAFT".equals(persisted.getStatus())) validateStructuralImmutability(persisted, period);
      if ("CLOSED".equals(persisted.getStatus()) && !sameNonStructural(persisted, period))
        throw error("Closed accounting period cannot be modified.");
    }
    if (!Boolean.TRUE.equals(period.getArchived()) && findDuplicateCode(period) != null)
      throw error("Accounting period code already exists.");
    if (!Boolean.TRUE.equals(period.getArchived()) && !findOverlappingPeriods(period).isEmpty())
      throw error("Accounting period overlaps another accounting period.");
  }

  protected AccountingPeriod findDuplicateCode(AccountingPeriod period) {
    String filter = "self.company = :company AND self.code = :code AND self.archived = false";
    var query = repository.all().filter(filter).bind("company", period.getCompany())
        .bind("code", period.getCode());
    if (period.getId() != null) query = repository.all().filter(filter + " AND self.id != :id")
        .bind("company", period.getCompany()).bind("code", period.getCode())
        .bind("id", period.getId());
    return query.fetchOne();
  }

  protected List<AccountingPeriod> findOverlappingPeriods(AccountingPeriod period) {
    String filter = "self.company = :company AND self.archived = false "
        + "AND self.startDate <= :endDate AND self.endDate >= :startDate";
    var query = repository.all().filter(filter).bind("company", period.getCompany())
        .bind("endDate", period.getEndDate()).bind("startDate", period.getStartDate());
    if (period.getId() != null) query = repository.all().filter(filter + " AND self.id != :id")
        .bind("company", period.getCompany()).bind("endDate", period.getEndDate())
        .bind("startDate", period.getStartDate()).bind("id", period.getId());
    return query.fetch();
  }

  protected List<AccountingPeriod> findApplicablePeriods(Company company, LocalDate date) {
    return repository.all().filter("self.company = :company AND self.archived = false "
        + "AND self.startDate <= :date AND self.endDate >= :date")
        .bind("company", company).bind("date", date).fetch();
  }

  protected <T> T withCompanyLock(Company company, Supplier<T> work) {
    if (company == null || company.getId() == null) throw error("Company is required.");
    JPA.em().find(Company.class, company.getId(), LockModeType.PESSIMISTIC_WRITE);
    return work.get();
  }

  protected AccountingPeriod lockRequired(AccountingPeriod value) {
    if (value == null || value.getId() == null) throw error("Persisted accounting period is required.");
    AccountingPeriod locked =
        JPA.em().find(AccountingPeriod.class, value.getId(), LockModeType.PESSIMISTIC_WRITE);
    if (locked == null) throw error("Accounting period does not exist.");
    return locked;
  }
  protected AccountingPeriod findPersisted(Long id) { return id == null ? null : repository.find(id); }
  protected AccountingPeriod persist(AccountingPeriod period) { return repository.save(period); }
  protected boolean canReopen() {
    var user = AuthUtils.getUser();
    if (user == null) return false;
    if (AuthUtils.isAdmin(user)) return true;
    boolean direct =
        user.getRoles() != null
            && user.getRoles().stream()
                .filter(Objects::nonNull)
                .flatMap(role -> role.getPermissions().stream())
                .anyMatch(
                    permission ->
                        "odc.accounting.period.reopen".equals(permission.getName()));
    if (direct || user.getGroup() == null || user.getGroup().getRoles() == null) return direct;
    return user.getGroup().getRoles().stream()
        .filter(Objects::nonNull)
        .flatMap(role -> role.getPermissions().stream())
        .anyMatch(
            permission -> "odc.accounting.period.reopen".equals(permission.getName()));
  }
  protected void requireReopenPermission() {
    if (!canReopen()) throw error("You do not have permission to reopen accounting periods.");
  }

  private void requireCompany(Company company, boolean activeContext) {
    if (company == null || Boolean.TRUE.equals(company.getArchived())
        || !Boolean.TRUE.equals(company.getActive())) throw error("Company must be active.");
    accessService.requireCompanyAccess(AuthUtils.getUser(), company);
    if (activeContext && !same(company, activeOrganizationService.requireActiveCompany()))
      throw error("You do not have access to the period company.");
  }
  private void validateStructuralImmutability(AccountingPeriod persisted, AccountingPeriod value) {
    if (!Objects.equals(persisted.getCode(), value.getCode())
        || !Objects.equals(persisted.getStartDate(), value.getStartDate())
        || !Objects.equals(persisted.getEndDate(), value.getEndDate()))
      throw error("Open or closed accounting period structural fields cannot be modified.");
  }
  private boolean sameNonStructural(AccountingPeriod persisted, AccountingPeriod value) {
    return Objects.equals(persisted.getName(), value.getName())
        && Objects.equals(persisted.getNotes(), value.getNotes())
        && Objects.equals(persisted.getSequence(), value.getSequence())
        && Objects.equals(persisted.getStatus(), value.getStatus());
  }
  private void defaults(AccountingPeriod period) {
    if (period.getArchived() == null) period.setArchived(false);
    if (period.getStatus() == null) period.setStatus("DRAFT");
    if (period.getSequence() == null) period.setSequence(0);
  }
  private String required(String value, String message) {
    if (value == null || value.trim().isEmpty()) throw error(message);
    return value.trim();
  }
  private boolean same(com.axelor.db.Model left, com.axelor.db.Model right) {
    return left == right || left != null && right != null && left.getId() != null
        && Objects.equals(left.getId(), right.getId());
  }
  private IllegalArgumentException error(String message) {
    return new IllegalArgumentException(I18n.get(message));
  }
}
