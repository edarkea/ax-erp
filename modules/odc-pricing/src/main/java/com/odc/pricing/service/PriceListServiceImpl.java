package com.odc.pricing.service;

import com.axelor.i18n.I18n;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import com.odc.organization.db.Company;
import com.odc.organization.service.ActiveOrganizationService;
import com.odc.pricing.db.PriceList;
import com.odc.pricing.db.repo.PriceListRepository;
import com.odc.reference.db.Currency;
import java.util.Objects;

public class PriceListServiceImpl implements PriceListService {

  private final PriceListRepository repository;
  private final ActiveOrganizationService organizationService;

  @Inject
  public PriceListServiceImpl(
      PriceListRepository repository, ActiveOrganizationService organizationService) {
    this.repository = repository;
    this.organizationService = organizationService;
  }

  @Override
  @Transactional
  public PriceList save(PriceList priceList) {
    validate(priceList);
    return persist(priceList);
  }

  @Override
  public void validate(PriceList priceList) {
    if (priceList == null) {
      throw error("Price list is required.");
    }

    Company activeCompany = organizationService.requireActiveCompany();
    if (priceList.getId() == null) {
      priceList.setCompany(activeCompany);
    }
    requireActiveCompany(priceList.getCompany());
    if (!sameCompany(priceList.getCompany(), activeCompany)) {
      throw error("You do not have access to the record company.");
    }

    priceList.setName(normalizeRequired(priceList.getName()));
    requireActiveCurrency(priceList.getCurrency());
    initializeDefaults(priceList);
    if (priceList.getPriority() < 0) {
      throw error("Priority cannot be negative.");
    }
    if (priceList.getValidFrom() != null
        && priceList.getValidUntil() != null
        && priceList.getValidUntil().isBefore(priceList.getValidFrom())) {
      throw error("Valid until cannot be before valid from.");
    }

    if (!Boolean.TRUE.equals(priceList.getArchived()) && findDuplicate(priceList) != null) {
      throw error("A price list with this name already exists.");
    }

    Company persistedCompany = findPersistedCompany(priceList.getId());
    if (persistedCompany != null && !sameCompany(persistedCompany, priceList.getCompany())) {
      throw error("Price list company cannot be changed.");
    }
  }

  @Override
  @Transactional
  public void archive(PriceList priceList) {
    requirePersisted(priceList);
    priceList.setActive(false);
    priceList.setArchived(true);
    persist(priceList);
  }

  @Override
  @Transactional
  public PriceList restore(PriceList priceList) {
    requirePersisted(priceList);
    priceList.setArchived(false);
    priceList.setActive(true);
    validate(priceList);
    return persist(priceList);
  }

  @Override
  public void requireUsable(PriceList priceList) {
    if (priceList == null
        || Boolean.TRUE.equals(priceList.getArchived())
        || !Boolean.TRUE.equals(priceList.getActive())) {
      throw error("Price list is archived or inactive.");
    }
    requireActiveCompany(priceList.getCompany());
    if (!sameCompany(priceList.getCompany(), organizationService.requireActiveCompany())) {
      throw error("You do not have access to the record company.");
    }
    requireActiveCurrency(priceList.getCurrency());
  }

  protected PriceList findDuplicate(PriceList priceList) {
    String filter =
        "self.company = :company AND self.name = :name AND self.archived = false";
    var query =
        repository
            .all()
            .filter(filter)
            .bind("company", priceList.getCompany())
            .bind("name", priceList.getName());
    if (priceList.getId() != null) {
      query =
          repository
              .all()
              .filter(filter + " AND self.id != :id")
              .bind("company", priceList.getCompany())
              .bind("name", priceList.getName())
              .bind("id", priceList.getId());
    }
    return query.fetchOne();
  }

  protected Company findPersistedCompany(Long id) {
    if (id == null) {
      return null;
    }
    PriceList persisted = repository.find(id);
    return persisted == null ? null : persisted.getCompany();
  }

  protected PriceList persist(PriceList priceList) {
    return repository.save(priceList);
  }

  private void requireActiveCompany(Company company) {
    if (company == null) {
      throw error("An active company must be selected.");
    }
    if (Boolean.TRUE.equals(company.getArchived()) || !Boolean.TRUE.equals(company.getActive())) {
      throw error("Company must be active.");
    }
  }

  private void requireActiveCurrency(Currency currency) {
    if (currency == null) {
      throw error("Currency is required.");
    }
    if (Boolean.TRUE.equals(currency.getArchived())) {
      throw error("Currency is archived.");
    }
  }

  private void initializeDefaults(PriceList priceList) {
    if (priceList.getArchived() == null) {
      priceList.setArchived(false);
    }
    if (priceList.getActive() == null) {
      priceList.setActive(true);
    }
    if (priceList.getPriority() == null) {
      priceList.setPriority(100);
    }
    if (priceList.getPricesIncludeTax() == null) {
      priceList.setPricesIncludeTax(false);
    }
  }

  private String normalizeRequired(String value) {
    if (value == null || value.trim().isEmpty()) {
      throw error("Price list name is required.");
    }
    return value.trim();
  }

  private boolean sameCompany(Company left, Company right) {
    return left == right
        || (left != null
            && right != null
            && left.getId() != null
            && Objects.equals(left.getId(), right.getId()));
  }

  private void requirePersisted(PriceList priceList) {
    if (priceList == null || priceList.getId() == null) {
      throw error("Price list is required.");
    }
  }

  private IllegalArgumentException error(String key) {
    return new IllegalArgumentException(I18n.get(key));
  }
}
