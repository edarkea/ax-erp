package com.odc.document.service;

import com.odc.document.db.EmissionEstablishment;
import com.odc.document.db.PointOfSale;

public interface EmissionConfigurationService {
  EmissionEstablishment save(EmissionEstablishment establishment);
  PointOfSale save(PointOfSale point);
  void validate(EmissionEstablishment establishment);
  void validate(PointOfSale point);
  void requireUsable(EmissionEstablishment establishment);
  void requireUsable(PointOfSale point);
}
