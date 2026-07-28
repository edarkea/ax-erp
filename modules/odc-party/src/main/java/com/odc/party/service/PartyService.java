package com.odc.party.service;

import com.odc.party.db.Party;

public interface PartyService {
  Party save(Party party);
  void validate(Party party);
  void archive(Party party);
  Party restore(Party party);
  void requireUsable(Party party);
}
