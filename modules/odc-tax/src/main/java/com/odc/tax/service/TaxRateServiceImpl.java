package com.odc.tax.service;

import com.axelor.db.JPA;
import com.axelor.i18n.I18n;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import com.odc.tax.db.TaxCategory;
import com.odc.tax.db.TaxRate;
import com.odc.tax.db.repo.TaxRateRepository;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class TaxRateServiceImpl implements TaxRateService {

  private final TaxRateRepository rateRepository;
  private final TaxCategoryService categoryService;

  @Inject
  public TaxRateServiceImpl(
      TaxRateRepository rateRepository, TaxCategoryService categoryService) {
    this.rateRepository = rateRepository;
    this.categoryService = categoryService;
  }

  @Override
  @Transactional
  public TaxRate save(TaxRate taxRate) {
    validate(taxRate);
    return persist(taxRate);
  }

  @Override
  @Transactional
  public void validate(TaxRate taxRate) {
    if (taxRate == null) {
      throw inconsistency("Tax rate is required.");
    }
    initializeArchived(taxRate);
    lockCategory(taxRate.getTaxCategory());
    if (taxRate.getRate() == null) {
      throw inconsistency("Tax rate is required.");
    }
    if (taxRate.getRate().signum() < 0) {
      throw inconsistency("Tax rate cannot be negative.");
    }
    if (taxRate.getValidFrom() == null) {
      throw inconsistency("Valid from date is required.");
    }
    if (taxRate.getValidUntil() != null
        && taxRate.getValidUntil().isBefore(taxRate.getValidFrom())) {
      throw inconsistency("Valid until date cannot be before valid from date.");
    }
    if (!Boolean.TRUE.equals(taxRate.getArchived()) && hasOverlap(taxRate)) {
      throw inconsistency("The validity period overlaps another tax rate.");
    }
    validateHistoricalIdentity(taxRate);
  }

  @Override
  @Transactional
  public TaxRate createRate(
      TaxCategory taxCategory, BigDecimal rate, LocalDate validFrom, LocalDate validUntil) {
    TaxRate taxRate = new TaxRate();
    taxRate.setTaxCategory(taxCategory);
    taxRate.setRate(rate);
    taxRate.setValidFrom(validFrom);
    taxRate.setValidUntil(validUntil);
    validate(taxRate);
    return persist(taxRate);
  }

  @Override
  @Transactional
  public TaxRate closeRate(TaxRate taxRate, LocalDate validUntil) {
    if (taxRate == null || taxRate.getId() == null) {
      throw inconsistency("Tax rate is required.");
    }
    TaxRate locked = findAndLockRate(taxRate.getId());
    if (locked == null || Boolean.TRUE.equals(locked.getArchived())) {
      throw inconsistency("Tax rate is archived.");
    }
    lockCategory(locked.getTaxCategory());
    locked.setValidUntil(validUntil);
    validate(locked);
    return persist(locked);
  }

  @Override
  public Optional<TaxRate> findApplicableRate(TaxCategory taxCategory, LocalDate date) {
    categoryService.requireUsable(taxCategory);
    if (date == null) {
      throw inconsistency("Tax date is required.");
    }
    List<TaxRate> rates = findApplicableRates(taxCategory, date);
    if (rates.size() > 1) {
      throw inconsistency("Multiple tax rates are applicable on the selected date.");
    }
    return rates.stream().findFirst();
  }

  @Override
  public TaxRate requireApplicableRate(TaxCategory taxCategory, LocalDate date) {
    return findApplicableRate(taxCategory, date)
        .orElseThrow(
            () -> inconsistency("No tax rate is applicable on the selected date."));
  }

  @Override
  @Transactional
  public void archive(TaxRate taxRate) {
    if (taxRate == null || taxRate.getId() == null) {
      throw inconsistency("Tax rate is required.");
    }
    taxRate.setArchived(true);
    persist(taxRate);
  }

  protected void lockCategory(TaxCategory category) {
    categoryService.requireUsable(category);
    if (category.getId() != null) {
      JPA.em().find(TaxCategory.class, category.getId(), LockModeType.PESSIMISTIC_WRITE);
    }
  }

  protected TaxRate findAndLockRate(Long id) {
    return JPA.em().find(TaxRate.class, id, LockModeType.PESSIMISTIC_WRITE);
  }

  protected TaxRate persist(TaxRate taxRate) {
    return rateRepository.save(taxRate);
  }

  protected Object[] findPersistedIdentity(Long id) {
    if (id == null) {
      return null;
    }
    return JPA.em()
        .createQuery(
            "SELECT self.taxCategory.id, self.rate, self.validFrom "
                + "FROM TaxRate self WHERE self.id = :id",
            Object[].class)
        .setParameter("id", id)
        .getResultStream()
        .findFirst()
        .orElse(null);
  }

  protected boolean hasOverlap(TaxRate taxRate) {
    String filter =
        "self.taxCategory = :category AND self.archived = false "
            + "AND self.validFrom <= :upperBound "
            + "AND (self.validUntil IS NULL OR self.validUntil >= :lowerBound)";
    LocalDate upperBound =
        taxRate.getValidUntil() == null ? LocalDate.of(9999, 12, 31) : taxRate.getValidUntil();
    var query =
        rateRepository
            .all()
            .filter(filter)
            .bind("category", taxRate.getTaxCategory())
            .bind("upperBound", upperBound)
            .bind("lowerBound", taxRate.getValidFrom());
    if (taxRate.getId() != null) {
      query =
          rateRepository
              .all()
              .filter(filter + " AND self.id != :id")
              .bind("category", taxRate.getTaxCategory())
              .bind("upperBound", upperBound)
              .bind("lowerBound", taxRate.getValidFrom())
              .bind("id", taxRate.getId());
    }
    return query.count() > 0;
  }

  protected List<TaxRate> findApplicableRates(TaxCategory category, LocalDate date) {
    return rateRepository
        .all()
        .filter(
            "self.taxCategory = :category AND self.archived = false "
                + "AND self.validFrom <= :date "
                + "AND (self.validUntil IS NULL OR self.validUntil >= :date)")
        .bind("category", category)
        .bind("date", date)
        .fetch(2);
  }

  private void validateHistoricalIdentity(TaxRate taxRate) {
    Object[] persisted = findPersistedIdentity(taxRate.getId());
    if (persisted == null) {
      return;
    }
    Long categoryId = taxRate.getTaxCategory().getId();
    if (categoryId == null || !categoryId.equals(persisted[0])) {
      throw inconsistency("Tax category cannot be changed on an existing tax rate.");
    }
    if (taxRate.getRate().compareTo((BigDecimal) persisted[1]) != 0) {
      throw inconsistency("Create a new validity period to change the tax rate.");
    }
    if (!taxRate.getValidFrom().equals(persisted[2])) {
      throw inconsistency("Valid from date cannot be changed on an existing tax rate.");
    }
  }

  private void initializeArchived(TaxRate taxRate) {
    if (taxRate.getArchived() == null) {
      taxRate.setArchived(false);
    }
  }

  private IllegalArgumentException inconsistency(String message) {
    return new IllegalArgumentException(I18n.get(message));
  }
}
