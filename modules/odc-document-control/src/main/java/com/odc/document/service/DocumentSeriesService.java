package com.odc.document.service;

import com.odc.document.db.DocumentSeries;

public interface DocumentSeriesService {
  DocumentSeries save(DocumentSeries series);
  void validate(DocumentSeries series);
  void requireUsable(DocumentSeries series);
}
