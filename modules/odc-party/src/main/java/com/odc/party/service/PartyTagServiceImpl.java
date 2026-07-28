package com.odc.party.service;

import com.axelor.i18n.I18n;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import com.odc.organization.db.Company;
import com.odc.organization.service.ActiveOrganizationService;
import com.odc.party.db.Party;
import com.odc.party.db.PartyTag;
import com.odc.party.db.PartyTagLink;
import com.odc.party.db.repo.PartyTagLinkRepository;
import com.odc.party.db.repo.PartyTagRepository;
import java.util.Objects;

public class PartyTagServiceImpl implements PartyTagService {
  private final PartyTagRepository tagRepository;
  private final PartyTagLinkRepository linkRepository;
  private final PartyService partyService;
  private final ActiveOrganizationService organizationService;
  @Inject
  public PartyTagServiceImpl(PartyTagRepository tagRepository, PartyTagLinkRepository linkRepository,
      PartyService partyService, ActiveOrganizationService organizationService) {
    this.tagRepository=tagRepository; this.linkRepository=linkRepository;
    this.partyService=partyService; this.organizationService=organizationService;
  }
  @Override @Transactional
  public PartyTag saveTag(PartyTag tag) {
    if (tag == null) throw error("Tag is required.");
    Company active = organizationService.requireActiveCompany();
    if (tag.getId() == null) tag.setCompany(active);
    validateTag(tag); return persistTag(tag);
  }
  @Override @Transactional
  public PartyTagLink link(Party party, PartyTag tag) {
    PartyTagLink link = new PartyTagLink(); link.setParty(party); link.setTag(tag);
    validateLink(link); return persistLink(link);
  }
  @Override @Transactional
  public void unlink(PartyTagLink link) {
    if (link == null || link.getId() == null) throw error("Tag link is required.");
    link.setArchived(true); persistLink(link);
  }
  @Override @Transactional
  public PartyTagLink restore(PartyTagLink link) {
    if (link == null || link.getId() == null) throw error("Tag link is required.");
    link.setArchived(false); validateLink(link); return persistLink(link);
  }
  @Override
  public void validateTag(PartyTag tag) {
    if (tag == null) throw error("Tag is required.");
    if (tag.getId() == null) tag.setCompany(organizationService.requireActiveCompany());
    if (tag.getCompany() == null) throw error("An active company must be selected.");
    Company active=organizationService.requireActiveCompany();
    if (!same(tag.getCompany(),active)) throw error("You do not have access to the record company.");
    if (Boolean.TRUE.equals(tag.getCompany().getArchived())
        || !Boolean.TRUE.equals(tag.getCompany().getActive())) throw error("Company must be active.");
    tag.setName(required(tag.getName(), "Tag name is required."));
    if (tag.getArchived()==null) tag.setArchived(false);
    if (tag.getActive()==null) tag.setActive(true);
    if (!Boolean.TRUE.equals(tag.getArchived()) && findDuplicateTag(tag)!=null) {
      throw error("Tag name already exists.");
    }
  }
  @Override
  public void validateLink(PartyTagLink link) {
    if (link==null || link.getTag()==null) throw error("Tag is required.");
    partyService.requireUsable(link.getParty());
    PartyTag tag=link.getTag();
    if (Boolean.TRUE.equals(tag.getArchived()) || !Boolean.TRUE.equals(tag.getActive())) {
      throw error("Tag is archived or inactive.");
    }
    if (!same(link.getParty().getCompany(),tag.getCompany())) throw error("Tag belongs to another company.");
    if (link.getArchived()==null) link.setArchived(false);
    if (!Boolean.TRUE.equals(link.getArchived()) && findDuplicateLink(link)!=null) {
      throw error("Tag is already linked to the party.");
    }
  }
  protected PartyTag findDuplicateTag(PartyTag tag) {
    String f="self.company = :company AND self.name = :name AND self.archived = false";
    var q=tagRepository.all().filter(f).bind("company",tag.getCompany()).bind("name",tag.getName());
    if(tag.getId()!=null)q=tagRepository.all().filter(f+" AND self.id != :id")
        .bind("company",tag.getCompany()).bind("name",tag.getName()).bind("id",tag.getId());
    return q.fetchOne();
  }
  protected PartyTagLink findDuplicateLink(PartyTagLink link) {
    String f="self.party = :party AND self.tag = :tag AND self.archived = false";
    var q=linkRepository.all().filter(f).bind("party",link.getParty()).bind("tag",link.getTag());
    if(link.getId()!=null)q=linkRepository.all().filter(f+" AND self.id != :id")
        .bind("party",link.getParty()).bind("tag",link.getTag()).bind("id",link.getId());
    return q.fetchOne();
  }
  protected PartyTag persistTag(PartyTag tag){return tagRepository.save(tag);}
  protected PartyTagLink persistLink(PartyTagLink link){return linkRepository.save(link);}
  private boolean same(Company a,Company b){return a==b||(a!=null&&b!=null&&a.getId()!=null&&Objects.equals(a.getId(),b.getId()));}
  private String required(String v,String key){if(v==null||v.trim().isEmpty())throw error(key);return v.trim();}
  private IllegalArgumentException error(String key){return new IllegalArgumentException(I18n.get(key));}
}
