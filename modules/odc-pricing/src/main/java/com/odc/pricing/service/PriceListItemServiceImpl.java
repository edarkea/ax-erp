package com.odc.pricing.service;

import com.axelor.i18n.I18n;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import com.odc.catalog.db.Item;
import com.odc.catalog.service.ItemService;
import com.odc.organization.db.Company;
import com.odc.pricing.db.PriceList;
import com.odc.pricing.db.PriceListItem;
import com.odc.pricing.db.repo.PriceListItemRepository;
import java.util.Objects;

public class PriceListItemServiceImpl implements PriceListItemService {

  private final PriceListItemRepository repository;
  private final PriceListService priceListService;
  private final ItemService itemService;

  @Inject
  public PriceListItemServiceImpl(
      PriceListItemRepository repository,
      PriceListService priceListService,
      ItemService itemService) {
    this.repository = repository;
    this.priceListService = priceListService;
    this.itemService = itemService;
  }

  @Override
  @Transactional
  public PriceListItem save(PriceListItem priceListItem) {
    validate(priceListItem);
    return persist(priceListItem);
  }

  @Override
  public void validate(PriceListItem priceListItem) {
    if (priceListItem == null) {
      throw error("Price list item is required.");
    }
    PriceList priceList = priceListItem.getPriceList();
    Item item = priceListItem.getItem();
    if (priceList == null) {
      throw error("Price list is required.");
    }
    if (item == null) {
      throw error("Item is required.");
    }

    priceListService.requireUsable(priceList);
    itemService.requireUsable(item);
    if (!sameCompany(priceList.getCompany(), item.getCompany())) {
      throw error("Item belongs to another company.");
    }
    if (priceListItem.getPrice() == null) {
      throw error("Price is required.");
    }
    if (priceListItem.getPrice().signum() < 0) {
      throw error("Price cannot be negative.");
    }
    validateRanges(priceListItem, priceList);
    initializeDefaults(priceListItem);

    if (!Boolean.TRUE.equals(priceListItem.getArchived())
        && findDuplicate(priceListItem) != null) {
      throw error("The item already has an active price in this list.");
    }

    PriceList persistedPriceList = findPersistedPriceList(priceListItem.getId());
    if (persistedPriceList != null && !sameRecord(persistedPriceList, priceList)) {
      throw error("Price list cannot be changed on an existing line.");
    }
  }

  @Override
  @Transactional
  public void archive(PriceListItem priceListItem) {
    requirePersisted(priceListItem);
    priceListItem.setActive(false);
    priceListItem.setArchived(true);
    persist(priceListItem);
  }

  @Override
  @Transactional
  public PriceListItem restore(PriceListItem priceListItem) {
    requirePersisted(priceListItem);
    priceListItem.setArchived(false);
    priceListItem.setActive(true);
    validate(priceListItem);
    return persist(priceListItem);
  }

  @Override
  public void requireUsable(PriceListItem priceListItem) {
    if (priceListItem == null
        || Boolean.TRUE.equals(priceListItem.getArchived())
        || !Boolean.TRUE.equals(priceListItem.getActive())) {
      throw error("Price list item is archived or inactive.");
    }
    validate(priceListItem);
  }

  protected PriceListItem findDuplicate(PriceListItem priceListItem) {
    String filter =
        "self.priceList = :priceList AND self.item = :item AND self.archived = false "
            + "AND ((self.minimumQuantity IS NULL AND :minimum IS NULL) "
            + "OR self.minimumQuantity = :minimum) "
            + "AND ((self.maximumQuantity IS NULL AND :maximum IS NULL) "
            + "OR self.maximumQuantity = :maximum) "
            + "AND ((self.validFrom IS NULL AND :validFrom IS NULL) "
            + "OR self.validFrom = :validFrom) "
            + "AND ((self.validUntil IS NULL AND :validUntil IS NULL) "
            + "OR self.validUntil = :validUntil)";
    var query =
        repository
            .all()
            .filter(filter)
            .bind("priceList", priceListItem.getPriceList())
            .bind("item", priceListItem.getItem())
            .bind("minimum", priceListItem.getMinimumQuantity())
            .bind("maximum", priceListItem.getMaximumQuantity())
            .bind("validFrom", priceListItem.getValidFrom())
            .bind("validUntil", priceListItem.getValidUntil());
    if (priceListItem.getId() != null) {
      query =
          repository
              .all()
              .filter(filter + " AND self.id != :id")
              .bind("priceList", priceListItem.getPriceList())
              .bind("item", priceListItem.getItem())
              .bind("minimum", priceListItem.getMinimumQuantity())
              .bind("maximum", priceListItem.getMaximumQuantity())
              .bind("validFrom", priceListItem.getValidFrom())
              .bind("validUntil", priceListItem.getValidUntil())
              .bind("id", priceListItem.getId());
    }
    return query.fetchOne();
  }

  protected PriceList findPersistedPriceList(Long id) {
    if (id == null) {
      return null;
    }
    PriceListItem persisted = repository.find(id);
    return persisted == null ? null : persisted.getPriceList();
  }

  protected PriceListItem persist(PriceListItem priceListItem) {
    return repository.save(priceListItem);
  }

  private void initializeDefaults(PriceListItem priceListItem) {
    if (priceListItem.getArchived() == null) {
      priceListItem.setArchived(false);
    }
    if (priceListItem.getActive() == null) {
      priceListItem.setActive(true);
    }
  }

  private void validateRanges(PriceListItem line, PriceList list) {
    if (line.getMinimumQuantity() != null && line.getMinimumQuantity().signum() < 0) {
      throw error("Minimum quantity cannot be negative.");
    }
    if (line.getMaximumQuantity() != null
        && (line.getMaximumQuantity().signum() < 0
            || (line.getMinimumQuantity() != null
                && line.getMaximumQuantity().signum() > 0
                && line.getMaximumQuantity().compareTo(line.getMinimumQuantity()) < 0))) {
      throw error("Maximum quantity must be greater than or equal to minimum quantity.");
    }
    if (line.getValidFrom() != null
        && line.getValidUntil() != null
        && line.getValidUntil().isBefore(line.getValidFrom())) {
      throw error("Valid until cannot be before valid from.");
    }
    if ((list.getValidFrom() != null
            && line.getValidFrom() != null
            && line.getValidFrom().isBefore(list.getValidFrom()))
        || (list.getValidUntil() != null
            && line.getValidUntil() != null
            && line.getValidUntil().isAfter(list.getValidUntil()))) {
      throw error("Line validity must be contained in price list validity.");
    }
  }

  private boolean sameCompany(Company left, Company right) {
    return left == right
        || (left != null
            && right != null
            && left.getId() != null
            && Objects.equals(left.getId(), right.getId()));
  }

  private boolean sameRecord(PriceList left, PriceList right) {
    return left == right
        || (left != null
            && right != null
            && left.getId() != null
            && Objects.equals(left.getId(), right.getId()));
  }

  private void requirePersisted(PriceListItem priceListItem) {
    if (priceListItem == null || priceListItem.getId() == null) {
      throw error("Price list item is required.");
    }
  }

  private IllegalArgumentException error(String key) {
    return new IllegalArgumentException(I18n.get(key));
  }
}
