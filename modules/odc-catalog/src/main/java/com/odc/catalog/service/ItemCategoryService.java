package com.odc.catalog.service;
import com.odc.catalog.db.ItemCategory;
public interface ItemCategoryService {
  ItemCategory save(ItemCategory category);
  void validate(ItemCategory category);
  void validateHierarchy(ItemCategory category);
  void archive(ItemCategory category);
  ItemCategory restore(ItemCategory category);
  void requireUsable(ItemCategory category);
}
