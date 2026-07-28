package com.odc.catalog.service;
import com.odc.catalog.db.UnitOfMeasure;
public interface UnitOfMeasureService {
  UnitOfMeasure save(UnitOfMeasure unit);
  void validate(UnitOfMeasure unit);
  void archive(UnitOfMeasure unit);
  UnitOfMeasure restore(UnitOfMeasure unit);
  void requireUsable(UnitOfMeasure unit);
}
