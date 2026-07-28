package com.odc.party.service;

import com.axelor.db.JPA;
import com.axelor.i18n.I18n;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import com.odc.party.db.Party;
import com.odc.party.db.PartyContactPoint;
import com.odc.party.db.repo.PartyContactPointRepository;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Locale;

public class PartyContactServiceImpl implements PartyContactService {
  private static final List<String> TYPES = List.of("EMAIL", "PHONE", "MOBILE", "WEB", "OTHER");
  private final PartyContactPointRepository repository;
  private final PartyService partyService;
  @Inject
  public PartyContactServiceImpl(PartyContactPointRepository repository, PartyService partyService) {
    this.repository = repository; this.partyService = partyService;
  }
  @Override @Transactional
  public PartyContactPoint save(PartyContactPoint contact) {
    validate(contact);
    lockParty(contact.getParty());
    if (Boolean.TRUE.equals(contact.getIsPrimary())) clearPrevious(contact);
    return persist(contact);
  }
  @Override
  public void validate(PartyContactPoint contact) {
    if (contact == null) throw error("Contact is required.");
    partyService.requireUsable(contact.getParty());
    if (contact.getType() == null || !TYPES.contains(contact.getType())) {
      throw error("Contact type is required.");
    }
    String value = contact.getValue() == null ? "" : contact.getValue().trim();
    if (value.isEmpty()) throw error("Contact value is required.");
    if ("EMAIL".equals(contact.getType())) {
      value = value.toLowerCase(Locale.ROOT);
      if (!value.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) throw error("Email format is invalid.");
    }
    if (("PHONE".equals(contact.getType()) || "MOBILE".equals(contact.getType()))
        && !value.matches("^[+0-9][0-9 ()-]{5,24}$")) throw error("Phone format is invalid.");
    contact.setValue(value);
    contact.setLabel(trim(contact.getLabel()));
    defaults(contact);
    if (Boolean.TRUE.equals(contact.getIsPrimary())
        && (Boolean.TRUE.equals(contact.getArchived()) || !Boolean.TRUE.equals(contact.getActive()))) {
      throw error("A primary contact must be active.");
    }
  }
  @Override @Transactional
  public void archive(PartyContactPoint contact) {
    if (contact == null || contact.getId() == null) throw error("Contact is required.");
    contact.setIsPrimary(false); contact.setActive(false); contact.setArchived(true); persist(contact);
  }
  protected void lockParty(Party party) {
    if (party.getId() != null) JPA.em().find(Party.class, party.getId(), LockModeType.PESSIMISTIC_WRITE);
  }
  protected void clearPrevious(PartyContactPoint contact) {
    repository.all().filter(
        "self.party = :party AND self.type = :type AND self.isPrimary = true "
            + "AND self.archived = false AND self.id != :id")
        .bind("party", contact.getParty()).bind("type", contact.getType())
        .bind("id", contact.getId() == null ? -1L : contact.getId()).fetch()
        .forEach(previous -> { previous.setIsPrimary(false); repository.save(previous); });
  }
  protected PartyContactPoint persist(PartyContactPoint contact) { return repository.save(contact); }
  private void defaults(PartyContactPoint c) {
    if (c.getArchived() == null) c.setArchived(false);
    if (c.getActive() == null) c.setActive(true);
    if (c.getIsPrimary() == null) c.setIsPrimary(false);
  }
  private String trim(String value) { return value == null || value.trim().isEmpty() ? null : value.trim(); }
  private IllegalArgumentException error(String key) { return new IllegalArgumentException(I18n.get(key)); }
}
