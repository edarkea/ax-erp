package com.odc.party.service;

import com.axelor.i18n.I18n;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import com.odc.organization.db.Company;
import com.odc.organization.service.ActiveOrganizationService;
import com.odc.party.db.Party;
import com.odc.party.db.repo.PartyRepository;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class PartyServiceImpl implements PartyService {
  private static final List<String> PARTY_TYPES = List.of("PERSON", "ORGANIZATION");
  private static final List<String> ID_TYPES =
      List.of("NATIONAL_ID", "TAX_ID", "PASSPORT", "FOREIGN_ID", "OTHER");
  private final PartyRepository repository;
  private final ActiveOrganizationService organizationService;

  @Inject
  public PartyServiceImpl(
      PartyRepository repository, ActiveOrganizationService organizationService) {
    this.repository = repository;
    this.organizationService = organizationService;
  }

  @Override
  @Transactional
  public Party save(Party party) {
    prepareCompany(party);
    validate(party);
    return persist(party);
  }

  @Override
  public void validate(Party party) {
    if (party == null) throw error("Party is required.");
    initializeDefaults(party);
    if (party.getId() == null) party.setCompany(organizationService.requireActiveCompany());
    requireCompany(party.getCompany());
    Company activeCompany = organizationService.requireActiveCompany();
    if (!same(party.getCompany(), activeCompany)) {
      throw error("You do not have access to the record company.");
    }
    if (party.getPartyType() == null || !PARTY_TYPES.contains(party.getPartyType())) {
      throw error("Party type is required.");
    }
    party.setDisplayName(normalizeRequired(party.getDisplayName(), "Display name is required."));
    party.setLegalName(normalizeOptional(party.getLegalName()));
    party.setTaxId(normalizeTaxId(party.getTaxId()));
    if (party.getTaxId() != null) {
      if (party.getTaxIdentificationType() == null
          || !ID_TYPES.contains(party.getTaxIdentificationType())) {
        throw error("Identification type is required.");
      }
      if (party.getTaxId().length() > 64) throw error("Identification is too long.");
      if (!Boolean.TRUE.equals(party.getArchived()) && findDuplicate(party) != null) {
        throw error("Party already exists for this identification.");
      }
    }
    validateCompanyChange(party);
  }

  @Override
  @Transactional
  public void archive(Party party) {
    requirePersisted(party);
    party.setArchived(true);
    persist(party);
  }

  @Override
  @Transactional
  public Party restore(Party party) {
    requirePersisted(party);
    party.setArchived(false);
    return save(party);
  }

  @Override
  public void requireUsable(Party party) {
    if (party == null || Boolean.TRUE.equals(party.getArchived())
        || !Boolean.TRUE.equals(party.getActive())) {
      throw error("Party is archived or inactive.");
    }
    requireCompany(party.getCompany());
    if (!same(party.getCompany(), organizationService.requireActiveCompany())) {
      throw error("You do not have access to the record company.");
    }
  }

  protected Party persist(Party party) {
    return repository.save(party);
  }

  protected Party findDuplicate(Party party) {
    String filter =
        "self.company = :company AND self.taxIdentificationType = :type "
            + "AND self.taxId = :taxId AND self.archived = false";
    var query = repository.all().filter(filter)
        .bind("company", party.getCompany())
        .bind("type", party.getTaxIdentificationType())
        .bind("taxId", party.getTaxId());
    if (party.getId() != null) {
      query = repository.all().filter(filter + " AND self.id != :id")
          .bind("company", party.getCompany())
          .bind("type", party.getTaxIdentificationType())
          .bind("taxId", party.getTaxId()).bind("id", party.getId());
    }
    return query.fetchOne();
  }

  protected Company findPersistedCompany(Long id) {
    return id == null ? null : repository.find(id).getCompany();
  }

  private void prepareCompany(Party party) {
    if (party == null) throw error("Party is required.");
    Company active = organizationService.requireActiveCompany();
    if (party.getId() == null) party.setCompany(active);
  }

  private void validateCompanyChange(Party party) {
    Company persisted = findPersistedCompany(party.getId());
    if (persisted != null && !same(persisted, party.getCompany())) {
      throw error("Party company cannot be changed.");
    }
  }

  private void requireCompany(Company company) {
    if (company == null) throw error("An active company must be selected.");
    if (Boolean.TRUE.equals(company.getArchived()) || !Boolean.TRUE.equals(company.getActive())) {
      throw error("Company must be active.");
    }
  }

  private boolean same(Company left, Company right) {
    return left == right || (left != null && right != null
        && left.getId() != null && Objects.equals(left.getId(), right.getId()));
  }

  private void initializeDefaults(Party party) {
    if (party.getArchived() == null) party.setArchived(false);
    if (party.getActive() == null) party.setActive(true);
  }

  private String normalizeRequired(String value, String message) {
    String normalized = normalizeOptional(value);
    if (normalized == null) throw error(message);
    return normalized;
  }

  private String normalizeOptional(String value) {
    if (value == null || value.trim().isEmpty()) return null;
    return value.trim();
  }

  private String normalizeTaxId(String value) {
    String normalized = normalizeOptional(value);
    return normalized == null ? null : normalized.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
  }

  private void requirePersisted(Party party) {
    if (party == null || party.getId() == null) throw error("Party is required.");
  }

  private IllegalArgumentException error(String key) {
    return new IllegalArgumentException(I18n.get(key));
  }
}
