package com.odc.party.service;

import com.axelor.db.JPA;
import com.axelor.i18n.I18n;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import com.odc.party.db.Party;
import com.odc.party.db.PartyAddress;
import com.odc.party.db.repo.PartyAddressRepository;
import com.odc.reference.db.City;
import jakarta.persistence.LockModeType;

public class PartyAddressServiceImpl implements PartyAddressService {
  private final PartyAddressRepository repository;
  private final PartyService partyService;
  @Inject
  public PartyAddressServiceImpl(PartyAddressRepository repository, PartyService partyService) {
    this.repository = repository; this.partyService = partyService;
  }
  @Override @Transactional
  public PartyAddress save(PartyAddress address) {
    validate(address); lockParty(address.getParty());
    if (Boolean.TRUE.equals(address.getIsBillingDefault())) clearDefault(address, true);
    if (Boolean.TRUE.equals(address.getIsShippingDefault())) clearDefault(address, false);
    return persist(address);
  }
  @Override
  public void validate(PartyAddress address) {
    if (address == null) throw error("Address is required.");
    partyService.requireUsable(address.getParty());
    address.setLine1(required(address.getLine1(), "Address line 1 is required."));
    address.setLine2(trim(address.getLine2())); address.setPostalCode(trim(address.getPostalCode()));
    defaults(address); validateCity(address.getCity());
    if ((Boolean.TRUE.equals(address.getIsBillingDefault())
        || Boolean.TRUE.equals(address.getIsShippingDefault()))
        && (Boolean.TRUE.equals(address.getArchived()) || !Boolean.TRUE.equals(address.getActive()))) {
      throw error("A default address must be active.");
    }
  }
  @Override @Transactional public void setBillingDefault(PartyAddress address) {
    address.setIsBillingDefault(true); save(address);
  }
  @Override @Transactional public void setShippingDefault(PartyAddress address) {
    address.setIsShippingDefault(true); save(address);
  }
  @Override @Transactional public void archive(PartyAddress address) {
    if (address == null || address.getId() == null) throw error("Address is required.");
    address.setIsBillingDefault(false); address.setIsShippingDefault(false);
    address.setActive(false); address.setArchived(true); persist(address);
  }
  protected void lockParty(Party party) {
    if (party.getId() != null) JPA.em().find(Party.class, party.getId(), LockModeType.PESSIMISTIC_WRITE);
  }
  protected void clearDefault(PartyAddress address, boolean billing) {
    String field = billing ? "isBillingDefault" : "isShippingDefault";
    repository.all().filter("self.party = :party AND self." + field
        + " = true AND self.archived = false AND self.id != :id")
        .bind("party", address.getParty()).bind("id", address.getId() == null ? -1L : address.getId())
        .fetch().forEach(previous -> {
          if (billing) previous.setIsBillingDefault(false); else previous.setIsShippingDefault(false);
          repository.save(previous);
        });
  }
  protected PartyAddress persist(PartyAddress address) { return repository.save(address); }
  private void validateCity(City city) {
    if (city == null) return;
    if (Boolean.TRUE.equals(city.getArchived()) || city.getState() == null
        || Boolean.TRUE.equals(city.getState().getArchived()) || city.getState().getCountry() == null
        || Boolean.TRUE.equals(city.getState().getCountry().getArchived())) {
      throw error("City and its geography must be active.");
    }
  }
  private void defaults(PartyAddress a) {
    if (a.getArchived() == null) a.setArchived(false);
    if (a.getActive() == null) a.setActive(true);
    if (a.getIsBillingDefault() == null) a.setIsBillingDefault(false);
    if (a.getIsShippingDefault() == null) a.setIsShippingDefault(false);
  }
  private String required(String v, String key) { String x=trim(v); if(x==null) throw error(key); return x; }
  private String trim(String v) { return v==null||v.trim().isEmpty()?null:v.trim(); }
  private IllegalArgumentException error(String key) { return new IllegalArgumentException(I18n.get(key)); }
}
