package com.odc.pricing.service;

import com.odc.pricing.db.PriceList;
import com.odc.pricing.db.PriceListItem;
import java.math.BigDecimal;

public record PriceResolution(
    PriceList priceList, PriceListItem priceListItem, BigDecimal price, boolean pricesIncludeTax) {}
