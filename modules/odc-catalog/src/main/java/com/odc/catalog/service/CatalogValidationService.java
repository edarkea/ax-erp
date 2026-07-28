package com.odc.catalog.service;
import com.odc.catalog.db.ItemCategory;
import com.odc.catalog.db.UnitOfMeasure;
import com.odc.organization.db.Company;
import com.odc.tax.db.TaxCategory;
public interface CatalogValidationService {
  void requireActiveCompany(Company company);
  void requireCategoryForCompany(ItemCategory category, Company company);
  void requireUsableUnit(UnitOfMeasure unit);
  void requireCompatibleTax(TaxCategory taxCategory, Company company);
}
