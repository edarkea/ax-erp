package com.odc.tax.service;

import com.axelor.db.JPA;
import com.axelor.i18n.I18n;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import com.odc.reference.db.Country;
import com.odc.tax.db.TaxCategory;
import com.odc.tax.db.repo.TaxCategoryRepository;
import java.util.List;
import java.util.Locale;

public class TaxCategoryServiceImpl implements TaxCategoryService {

  private static final List<String> TYPES = List.of("VAT", "WITHHOLDING", "EXCISE", "OTHER");

  private final TaxCategoryRepository categoryRepository;

  @Inject
  public TaxCategoryServiceImpl(TaxCategoryRepository categoryRepository) {
    this.categoryRepository = categoryRepository;
  }

  @Override
  @Transactional
  public TaxCategory save(TaxCategory category) {
    validate(category);
    return persist(category);
  }

  @Override
  public void validate(TaxCategory category) {
    if (category == null) {
      throw inconsistency("Tax category is required.");
    }
    initializeArchived(category);
    requireUsableCountry(category.getCountry());
    category.setCode(normalizeRequired(category.getCode(), "Tax code is required.", true));
    category.setName(normalizeRequired(category.getName(), "Tax category name is required.", false));
    if (category.getType() == null || !TYPES.contains(category.getType())) {
      throw inconsistency("Tax type is required.");
    }
    if (!Boolean.TRUE.equals(category.getArchived())
        && findOtherActive(category) != null) {
      throw inconsistency("Tax category already exists.");
    }
    validateStructuralChanges(category);
  }

  @Override
  @Transactional
  public void archive(TaxCategory category) {
    if (category == null || category.getId() == null) {
      throw inconsistency("Tax category is required.");
    }
    category.setArchived(true);
    persist(category);
  }

  @Override
  public void requireUsable(TaxCategory category) {
    if (category == null) {
      throw inconsistency("Tax category is required.");
    }
    if (Boolean.TRUE.equals(category.getArchived())) {
      throw inconsistency("Tax category is archived.");
    }
    requireUsableCountry(category.getCountry());
  }

  protected TaxCategory findOtherActive(TaxCategory category) {
    String filter =
        "self.country = :country AND self.type = :type AND self.code = :code "
            + "AND self.archived = false";
    var query =
        categoryRepository
            .all()
            .filter(filter)
            .bind("country", category.getCountry())
            .bind("type", category.getType())
            .bind("code", category.getCode());
    if (category.getId() != null) {
      query =
          categoryRepository
              .all()
              .filter(filter + " AND self.id != :id")
              .bind("country", category.getCountry())
              .bind("type", category.getType())
              .bind("code", category.getCode())
              .bind("id", category.getId());
    }
    return query.fetchOne();
  }

  protected TaxCategory persist(TaxCategory category) {
    return categoryRepository.save(category);
  }

  protected Object[] findPersistedStructure(Long id) {
    if (id == null) {
      return null;
    }
    return JPA.em()
        .createQuery(
            "SELECT self.country.id, self.type FROM TaxCategory self WHERE self.id = :id",
            Object[].class)
        .setParameter("id", id)
        .getResultStream()
        .findFirst()
        .orElse(null);
  }

  protected boolean hasRates(TaxCategory category) {
    return category.getId() != null
        && categoryRepository
                .all()
                .filter("self.id = :id AND EXISTS (SELECT rate.id FROM TaxRate rate "
                    + "WHERE rate.taxCategory = self)")
                .bind("id", category.getId())
                .count()
            > 0;
  }

  private void validateStructuralChanges(TaxCategory category) {
    if (category.getId() == null || !hasRates(category)) {
      return;
    }
    Object[] persisted = findPersistedStructure(category.getId());
    if (persisted == null) {
      return;
    }
    Long countryId = category.getCountry().getId();
    if (countryId == null || !countryId.equals(persisted[0])) {
      throw inconsistency("Country cannot be changed on a tax category that contains rates.");
    }
    if (!category.getType().equals(persisted[1])) {
      throw inconsistency("Type cannot be changed on a tax category that contains rates.");
    }
  }

  private void requireUsableCountry(Country country) {
    if (country == null) {
      throw inconsistency("Country is required.");
    }
    if (Boolean.TRUE.equals(country.getArchived())) {
      throw inconsistency("Country must be active.");
    }
  }

  private String normalizeRequired(String value, String message, boolean uppercase) {
    String normalized = value == null ? null : value.trim();
    if (normalized == null || normalized.isEmpty()) {
      throw inconsistency(message);
    }
    return uppercase ? normalized.toUpperCase(Locale.ROOT) : normalized;
  }

  private void initializeArchived(TaxCategory category) {
    if (category.getArchived() == null) {
      category.setArchived(false);
    }
  }

  private IllegalArgumentException inconsistency(String message) {
    return new IllegalArgumentException(I18n.get(message));
  }
}
