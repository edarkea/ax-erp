package com.odc.party.service;

import com.odc.party.db.Party;
import com.odc.party.db.PartyRole;

public interface PartyRoleService {
  PartyRole assignRole(Party party, String roleType);
  void removeRole(PartyRole role);
  boolean hasRole(Party party, String roleType);
  void requireRole(Party party, String roleType);
  void validate(PartyRole role);
}
