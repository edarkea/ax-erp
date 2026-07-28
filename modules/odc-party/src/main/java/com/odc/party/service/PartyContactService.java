package com.odc.party.service;
import com.odc.party.db.PartyContactPoint;
public interface PartyContactService {
  PartyContactPoint save(PartyContactPoint contact);
  void validate(PartyContactPoint contact);
  void archive(PartyContactPoint contact);
}
