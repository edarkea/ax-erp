package com.odc.organization.context;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import java.util.Optional;

public class AxelorCurrentUserProvider implements CurrentUserProvider {
  @Override
  public Optional<User> getCurrentUser() {
    return Optional.ofNullable(AuthUtils.getUser());
  }
}
