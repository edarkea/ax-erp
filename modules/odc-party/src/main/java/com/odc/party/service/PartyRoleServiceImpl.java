package com.odc.party.service;

import com.axelor.i18n.I18n;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import com.odc.party.db.Party;
import com.odc.party.db.PartyRole;
import com.odc.party.db.repo.PartyRoleRepository;
import java.util.List;

public class PartyRoleServiceImpl implements PartyRoleService {
  private static final List<String> TYPES = List.of("CUSTOMER", "SUPPLIER", "EMPLOYEE", "OTHER");
  private final PartyRoleRepository repository;
  private final PartyService partyService;

  @Inject
  public PartyRoleServiceImpl(PartyRoleRepository repository, PartyService partyService) {
    this.repository = repository;
    this.partyService = partyService;
  }

  @Override
  @Transactional
  public PartyRole assignRole(Party party, String roleType) {
    PartyRole role = new PartyRole();
    role.setParty(party);
    role.setRoleType(roleType);
    validate(role);
    return persist(role);
  }

  @Override
  @Transactional
  public void removeRole(PartyRole role) {
    if (role == null || role.getId() == null) throw error("Party role is required.");
    role.setActive(false);
    role.setArchived(true);
    persist(role);
  }

  @Override
  public boolean hasRole(Party party, String roleType) {
    return repository.all().filter(
        "self.party = :party AND self.roleType = :type AND self.archived = false "
            + "AND self.active = true")
        .bind("party", party).bind("type", roleType).count() > 0;
  }

  @Override
  public void requireRole(Party party, String roleType) {
    if (!hasRole(party, roleType)) throw error("Party does not have the required role.");
  }

  @Override
  public void validate(PartyRole role) {
    if (role == null) throw error("Party role is required.");
    partyService.requireUsable(role.getParty());
    if (role.getRoleType() == null || !TYPES.contains(role.getRoleType())) {
      throw error("Role type is required.");
    }
    if (role.getArchived() == null) role.setArchived(false);
    if (role.getActive() == null) role.setActive(true);
    if (!Boolean.TRUE.equals(role.getArchived()) && findDuplicate(role) != null) {
      throw error("Party role is already assigned.");
    }
  }

  protected PartyRole findDuplicate(PartyRole role) {
    String filter =
        "self.party = :party AND self.roleType = :type AND self.archived = false";
    var query = repository.all().filter(filter)
        .bind("party", role.getParty()).bind("type", role.getRoleType());
    if (role.getId() != null) {
      query = repository.all().filter(filter + " AND self.id != :id")
          .bind("party", role.getParty()).bind("type", role.getRoleType())
          .bind("id", role.getId());
    }
    return query.fetchOne();
  }

  protected PartyRole persist(PartyRole role) {
    return repository.save(role);
  }

  private IllegalArgumentException error(String key) {
    return new IllegalArgumentException(I18n.get(key));
  }
}
