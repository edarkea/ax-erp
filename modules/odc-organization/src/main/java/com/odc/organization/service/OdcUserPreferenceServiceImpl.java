package com.odc.organization.service;

import com.axelor.auth.db.User;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import com.odc.organization.db.OdcUserPreference;
import com.odc.organization.db.repo.OdcUserPreferenceRepository;
import java.util.Objects;
import java.util.Set;

public class OdcUserPreferenceServiceImpl implements OdcUserPreferenceService {

  private static final Set<String> THEMES = Set.of("SYSTEM", "LIGHT", "DARK");
  private static final Set<String> NAVIGATION_MODES = Set.of("SIDEBAR", "COMPACT", "EXPANDED");

  private final OdcUserPreferenceRepository repository;
  private final OrganizationValidationService organizationValidation;
  private final AccessValidationService accessValidation;

  @Inject
  public OdcUserPreferenceServiceImpl(
      OdcUserPreferenceRepository repository,
      OrganizationValidationService organizationValidation,
      AccessValidationService accessValidation) {
    this.repository = repository;
    this.organizationValidation = organizationValidation;
    this.accessValidation = accessValidation;
  }

  @Override
  @Transactional
  public OdcUserPreference getOrCreate(User user) {
    accessValidation.requireUsable(user);
    OdcUserPreference existing = findAnyByUser(user);
    if (existing != null) {
      if (Boolean.TRUE.equals(existing.getArchived())) {
        existing.setArchived(false);
        return persist(existing);
      }
      return existing;
    }
    OdcUserPreference preference = new OdcUserPreference();
    preference.setUser(user);
    preference.setTheme("SYSTEM");
    preference.setNavigationMode("SIDEBAR");
    preference.setArchived(false);
    return persist(preference);
  }

  @Override
  @Transactional
  public OdcUserPreference save(OdcUserPreference preference) {
    validate(preference);
    return persist(preference);
  }

  @Override
  public void validate(OdcUserPreference preference) {
    if (preference == null) throw accessValidation.error("User preference is required.");
    if (preference.getArchived() == null) preference.setArchived(false);
    accessValidation.requireUsable(preference.getUser());
    if (!THEMES.contains(preference.getTheme())) {
      throw accessValidation.error("Theme selection is invalid.");
    }
    if (!NAVIGATION_MODES.contains(preference.getNavigationMode())) {
      throw accessValidation.error("Navigation mode selection is invalid.");
    }
    preference.setLocale(organizationValidation.normalizeLocale(preference.getLocale()));
    if (preference.getProfileImage() != null) {
      String type = preference.getProfileImage().getFileType();
      Long size = preference.getProfileImage().getFileSize();
      if (type == null || !Set.of("image/jpeg", "image/png", "image/webp").contains(type)) {
        throw accessValidation.error("Profile image type is invalid.");
      }
      if (size == null || size > 4L * 1024 * 1024) {
        throw accessValidation.error("Profile image must not exceed 4 MB.");
      }
    }
    OdcUserPreference other = findByUser(preference.getUser());
    if (other != null
        && other != preference
        && !Objects.equals(other.getId(), preference.getId())) {
      throw accessValidation.error("The user already has ODC preferences.");
    }
  }

  @Override
  @Transactional
  public void archive(OdcUserPreference preference) {
    if (preference == null) throw accessValidation.error("User preference is required.");
    preference.setArchived(true);
    persist(preference);
  }

  protected OdcUserPreference findByUser(User user) {
    return repository
        .all()
        .filter("self.user = :user AND self.archived = false")
        .bind("user", user)
        .fetchOne();
  }

  protected OdcUserPreference findAnyByUser(User user) {
    return repository.all().filter("self.user = :user").bind("user", user).fetchOne();
  }

  protected OdcUserPreference persist(OdcUserPreference preference) {
    return repository.save(preference);
  }
}
