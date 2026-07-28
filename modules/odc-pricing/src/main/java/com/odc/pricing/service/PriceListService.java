package com.odc.pricing.service;

import com.odc.pricing.db.PriceList;

public interface PriceListService {

  PriceList save(PriceList priceList);

  void validate(PriceList priceList);

  void archive(PriceList priceList);

  PriceList restore(PriceList priceList);

  void requireUsable(PriceList priceList);
}
