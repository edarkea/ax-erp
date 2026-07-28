package com.odc.tax.service;

import com.odc.tax.db.TaxCategory;

public interface TaxCategoryService {

  TaxCategory save(TaxCategory category);

  void validate(TaxCategory category);

  void archive(TaxCategory category);

  void requireUsable(TaxCategory category);
}
