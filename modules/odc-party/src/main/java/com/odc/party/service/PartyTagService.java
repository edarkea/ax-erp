package com.odc.party.service;
import com.odc.party.db.Party;
import com.odc.party.db.PartyTag;
import com.odc.party.db.PartyTagLink;
public interface PartyTagService {
  PartyTag saveTag(PartyTag tag);
  PartyTagLink link(Party party, PartyTag tag);
  void unlink(PartyTagLink link);
  PartyTagLink restore(PartyTagLink link);
  void validateTag(PartyTag tag);
  void validateLink(PartyTagLink link);
}
