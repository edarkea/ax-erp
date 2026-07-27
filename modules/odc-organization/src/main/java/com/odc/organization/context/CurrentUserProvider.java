package com.odc.organization.context;

import com.axelor.auth.db.User;
import java.util.Optional;

public interface CurrentUserProvider {
  Optional<User> getCurrentUser();
}
