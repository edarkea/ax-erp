package com.odc.pricing.service;

import com.odc.catalog.db.Item;
import com.odc.organization.db.Company;
import com.odc.pricing.db.PriceList;
import com.odc.reference.db.Currency;
import java.math.BigDecimal;
import java.time.LocalDate;

public interface PriceResolverService {

  PriceResolution resolve(
      Company company,
      Item item,
      Currency currency,
      LocalDate date,
      BigDecimal quantity,
      PriceList explicitPriceList);
}
