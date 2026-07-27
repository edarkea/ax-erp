package com.odc.organization.service;

import com.axelor.auth.db.User;
import com.odc.organization.db.OdcUserPreference;

public interface OdcUserPreferenceService {
  OdcUserPreference getOrCreate(User user);
  OdcUserPreference save(OdcUserPreference preference);
  void validate(OdcUserPreference preference);
  void archive(OdcUserPreference preference);
}
