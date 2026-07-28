package com.odc.party.service;
import com.odc.party.db.PartyAddress;
public interface PartyAddressService {
  PartyAddress save(PartyAddress address);
  void setBillingDefault(PartyAddress address);
  void setShippingDefault(PartyAddress address);
  void archive(PartyAddress address);
  void validate(PartyAddress address);
}
