package com.odc.organization.service;

import com.axelor.auth.db.User;
import com.axelor.i18n.I18n;
import java.time.LocalDateTime;

public class AccessValidationService {

  public void requireUsable(User user) {
    if (user == null) {
      throw error("User is required.");
    }
    LocalDateTime now = LocalDateTime.now();
    if (Boolean.TRUE.equals(user.getArchived())
        || Boolean.TRUE.equals(user.getBlocked())
        || (user.getActivateOn() != null && user.getActivateOn().isAfter(now))
        || (user.getExpiresOn() != null && !user.getExpiresOn().isAfter(now))) {
      throw error("User must be active and not archived.");
    }
  }

  public IllegalArgumentException error(String message, Object... args) {
    String translated = I18n.get(message);
    for (int index = 0; index < args.length; index++) {
      translated = translated.replace("{" + index + "}", String.valueOf(args[index]));
    }
    return new IllegalArgumentException(translated);
  }
}
