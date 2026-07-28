package com.odc.pricing.service;

import com.axelor.i18n.I18n;
import com.google.inject.Inject;
import com.odc.catalog.db.Item;
import com.odc.catalog.service.ItemService;
import com.odc.organization.db.Company;
import com.odc.pricing.db.PriceList;
import com.odc.pricing.db.PriceListItem;
import com.odc.pricing.db.repo.PriceListItemRepository;
import com.odc.reference.db.Currency;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class PriceResolverServiceImpl implements PriceResolverService {

  private final PriceListItemRepository repository;
  private final PriceListService priceListService;
  private final ItemService itemService;

  @Inject
  public PriceResolverServiceImpl(
      PriceListItemRepository repository,
      PriceListService priceListService,
      ItemService itemService) {
    this.repository = repository;
    this.priceListService = priceListService;
    this.itemService = itemService;
  }

  @Override
  public PriceResolution resolve(
      Company company,
      Item item,
      Currency currency,
      LocalDate date,
      BigDecimal quantity,
      PriceList explicitPriceList) {
    if (company == null || currency == null || item == null) {
      throw error("Company, item and currency are required to resolve a price.");
    }
    itemService.requireUsable(item);
    if (!same(company, item.getCompany())) {
      throw error("Item belongs to another company.");
    }
    LocalDate effectiveDate = date == null ? LocalDate.now() : date;
    BigDecimal effectiveQuantity = quantity == null ? BigDecimal.ONE : quantity;
    if (effectiveQuantity.signum() <= 0) {
      throw error("Quantity must be greater than zero.");
    }
    if (explicitPriceList != null) {
      priceListService.requireUsable(explicitPriceList);
      if (!same(company, explicitPriceList.getCompany())
          || !same(currency, explicitPriceList.getCurrency())
          || !validOn(explicitPriceList, effectiveDate)) {
        throw error("Explicit price list is not valid for the requested context.");
      }
    }

    List<PriceListItem> candidates =
        findCandidates(company, item, currency, effectiveDate, effectiveQuantity, explicitPriceList)
            .stream()
            .filter(line -> matches(line, effectiveDate, effectiveQuantity))
            .sorted(candidateOrder())
            .toList();
    if (candidates.isEmpty()) {
      throw error("No applicable price was found.");
    }
    PriceListItem selected = candidates.get(0);
    if (candidates.size() > 1 && equivalentRank(selected, candidates.get(1))) {
      throw error("More than one price rule has the same priority.");
    }
    return new PriceResolution(
        selected.getPriceList(),
        selected,
        selected.getPrice(),
        Boolean.TRUE.equals(selected.getPriceList().getPricesIncludeTax()));
  }

  protected List<PriceListItem> findCandidates(
      Company company,
      Item item,
      Currency currency,
      LocalDate date,
      BigDecimal quantity,
      PriceList explicitPriceList) {
    String filter =
        "self.item = :item AND self.archived = false AND self.active = true "
            + "AND self.priceList.company = :company AND self.priceList.currency = :currency "
            + "AND self.priceList.archived = false AND self.priceList.active = true";
    var query =
        repository
            .all()
            .filter(filter)
            .bind("item", item)
            .bind("company", company)
            .bind("currency", currency);
    if (explicitPriceList != null) {
      query =
          repository
              .all()
              .filter(filter + " AND self.priceList = :priceList")
              .bind("item", item)
              .bind("company", company)
              .bind("currency", currency)
              .bind("priceList", explicitPriceList);
    }
    return query.fetch();
  }

  private boolean matches(PriceListItem line, LocalDate date, BigDecimal quantity) {
    return line != null
        && !Boolean.TRUE.equals(line.getArchived())
        && Boolean.TRUE.equals(line.getActive())
        && validOn(line.getPriceList(), date)
        && (line.getValidFrom() == null || !date.isBefore(line.getValidFrom()))
        && (line.getValidUntil() == null || !date.isAfter(line.getValidUntil()))
        && (line.getMinimumQuantity() == null
            || quantity.compareTo(line.getMinimumQuantity()) >= 0)
        && (line.getMaximumQuantity() == null
            || line.getMaximumQuantity().signum() == 0
            || quantity.compareTo(line.getMaximumQuantity()) <= 0);
  }

  private boolean validOn(PriceList list, LocalDate date) {
    return list != null
        && !Boolean.TRUE.equals(list.getArchived())
        && Boolean.TRUE.equals(list.getActive())
        && (list.getValidFrom() == null || !date.isBefore(list.getValidFrom()))
        && (list.getValidUntil() == null || !date.isAfter(list.getValidUntil()));
  }

  private Comparator<PriceListItem> candidateOrder() {
    return Comparator.comparing(
            (PriceListItem line) ->
                rangeWidth(line) == null ? BigDecimal.valueOf(Long.MAX_VALUE) : rangeWidth(line))
        .thenComparing(
            line ->
                line.getMinimumQuantity() == null
                    ? BigDecimal.ZERO
                    : line.getMinimumQuantity(),
            Comparator.reverseOrder())
        .thenComparing(line -> line.getPriceList().getPriority());
  }

  private BigDecimal rangeWidth(PriceListItem line) {
    if (line.getMinimumQuantity() == null
        || line.getMaximumQuantity() == null
        || line.getMaximumQuantity().signum() == 0) {
      return null;
    }
    return line.getMaximumQuantity().subtract(line.getMinimumQuantity());
  }

  private boolean equivalentRank(PriceListItem left, PriceListItem right) {
    return Objects.equals(rangeWidth(left), rangeWidth(right))
        && Objects.equals(left.getMinimumQuantity(), right.getMinimumQuantity())
        && Objects.equals(left.getPriceList().getPriority(), right.getPriceList().getPriority());
  }

  private boolean same(com.axelor.db.Model left, com.axelor.db.Model right) {
    return left == right
        || (left != null
            && right != null
            && left.getId() != null
            && Objects.equals(left.getId(), right.getId()));
  }

  private IllegalArgumentException error(String message) {
    return new IllegalArgumentException(I18n.get(message));
  }
}
