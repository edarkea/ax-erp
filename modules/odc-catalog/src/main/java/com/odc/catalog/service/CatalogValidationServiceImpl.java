package com.odc.catalog.service;

import com.axelor.i18n.I18n;
import com.google.inject.Inject;
import com.odc.catalog.db.ItemCategory;
import com.odc.catalog.db.UnitOfMeasure;
import com.odc.organization.db.Company;
import com.odc.tax.db.TaxCategory;
import com.odc.tax.service.TaxCategoryService;
import java.util.Objects;

public class CatalogValidationServiceImpl implements CatalogValidationService {
  private final UnitOfMeasureService unitService;
  private final ItemCategoryService categoryService;
  private final TaxCategoryService taxService;
  @Inject public CatalogValidationServiceImpl(UnitOfMeasureService unitService,
      ItemCategoryService categoryService,TaxCategoryService taxService){
    this.unitService=unitService;this.categoryService=categoryService;this.taxService=taxService;}
  @Override public void requireActiveCompany(Company company){
    if(company==null)throw error("An active company must be selected.");
    if(Boolean.TRUE.equals(company.getArchived())||!Boolean.TRUE.equals(company.getActive()))
      throw error("Company must be active.");}
  @Override public void requireCategoryForCompany(ItemCategory category,Company company){
    if(category==null)return;categoryService.requireUsable(category);
    if(!same(category.getCompany(),company))throw error("Category belongs to another company.");}
  @Override public void requireUsableUnit(UnitOfMeasure unit){if(unit!=null)unitService.requireUsable(unit);}
  @Override public void requireCompatibleTax(TaxCategory taxCategory,Company company){
    if(taxCategory==null)return;taxService.requireUsable(taxCategory);
    if(company.getCountry()==null)throw error("Company country is required when a tax category is selected.");
    if(taxCategory.getCountry()==null||!Objects.equals(company.getCountry().getId(),taxCategory.getCountry().getId()))
      throw error("Tax category does not match the company country.");}
  private boolean same(Company a,Company b){return a==b||(a!=null&&b!=null&&a.getId()!=null&&Objects.equals(a.getId(),b.getId()));}
  private IllegalArgumentException error(String key){return new IllegalArgumentException(I18n.get(key));}
}
