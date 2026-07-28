package com.odc.catalog.service;
import com.odc.catalog.db.Item;
public interface ItemService {
  Item save(Item item);
  void validate(Item item);
  void archive(Item item);
  Item restore(Item item);
  void requireUsable(Item item);
}
