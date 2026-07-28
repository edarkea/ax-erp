package com.odc.party.observer;
import static com.odc.common.rpc.RequestEntityUtils.process;
import com.axelor.event.Observes;
import com.axelor.events.PreRequest;
import com.axelor.events.RequestEvent;
import com.axelor.events.qualifiers.EntityType;
import com.odc.party.db.*;
import com.odc.party.service.*;
import jakarta.inject.Inject;
import jakarta.inject.Named;
public class PartySaveObserver {
  private final PartyService parties; private final PartyRoleService roles;
  private final PartyContactService contacts; private final PartyAddressService addresses;
  private final PartyTagService tags;
  @Inject public PartySaveObserver(PartyService p,PartyRoleService r,PartyContactService c,
      PartyAddressService a,PartyTagService t){parties=p;roles=r;contacts=c;addresses=a;tags=t;}
  public void party(@Observes @Named(RequestEvent.SAVE) @EntityType(Party.class) PreRequest e){
    process(e,Party.class,parties::validate);}
  public void role(@Observes @Named(RequestEvent.SAVE) @EntityType(PartyRole.class) PreRequest e){
    process(e,PartyRole.class,roles::validate);}
  public void contact(@Observes @Named(RequestEvent.SAVE) @EntityType(PartyContactPoint.class) PreRequest e){
    process(e,PartyContactPoint.class,contacts::save);}
  public void address(@Observes @Named(RequestEvent.SAVE) @EntityType(PartyAddress.class) PreRequest e){
    process(e,PartyAddress.class,addresses::save);}
  public void tag(@Observes @Named(RequestEvent.SAVE) @EntityType(PartyTag.class) PreRequest e){
    process(e,PartyTag.class,tags::validateTag);}
  public void tagLink(@Observes @Named(RequestEvent.SAVE) @EntityType(PartyTagLink.class) PreRequest e){
    process(e,PartyTagLink.class,tags::validateLink);}
}
