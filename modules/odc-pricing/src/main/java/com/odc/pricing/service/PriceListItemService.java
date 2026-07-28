package com.odc.pricing.service;

import com.odc.pricing.db.PriceListItem;

public interface PriceListItemService {

  PriceListItem save(PriceListItem priceListItem);

  void validate(PriceListItem priceListItem);

  void archive(PriceListItem priceListItem);

  PriceListItem restore(PriceListItem priceListItem);

  void requireUsable(PriceListItem priceListItem);
}
