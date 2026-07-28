package com.odc.party.service;

import static org.junit.jupiter.api.Assertions.*;

import com.odc.organization.context.ActiveOrganizationContext;
import com.odc.organization.db.Branch;
import com.odc.organization.db.Company;
import com.odc.organization.service.ActiveOrganizationService;
import com.odc.party.db.*;
import com.odc.reference.db.City;
import com.odc.reference.db.Country;
import com.odc.reference.db.State;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

class PartyChildrenServicesTest {
  @Test void validatesEmailPhoneAndEmptyContact(){
    TestContactService s=new TestContactService();Party p=party(company(1L));
    PartyContactPoint email=contact(p,"EMAIL"," USER@EXAMPLE.COM ");s.validate(email);
    assertEquals("user@example.com",email.getValue());
    s.validate(contact(p,"PHONE","+593 999 999"));
    assertThrows(IllegalArgumentException.class,()->s.validate(contact(p,"EMAIL","bad")));
    assertThrows(IllegalArgumentException.class,()->s.validate(contact(p,"PHONE","")));
  }
  @Test void primaryContactUsesAggregateLockAndClearsPrevious() throws Exception{
    TestContactService s=new TestContactService();Party p=party(company(1L));
    PartyContactPoint a=contact(p,"EMAIL","a@x.com"),b=contact(p,"EMAIL","b@x.com");
    a.setIsPrimary(true);b.setIsPrimary(true);ExecutorService ex=Executors.newFixedThreadPool(2);
    try{Future<?>f1=ex.submit(()->s.save(a));Future<?>f2=ex.submit(()->s.save(b));f1.get();f2.get();
      assertEquals(2,s.locks);assertEquals(2,s.clears);}finally{ex.shutdownNow();}
    assertFalse(hasMethod(PartyContactPoint.class,"getCompany"));
  }
  @Test void validatesAddressGeographyAndBothDefaults(){
    TestAddressService s=new TestAddressService();PartyAddress a=new PartyAddress();
    a.setParty(party(company(1L)));a.setLine1(" Main ");a.setCity(city(false));a.setIsBillingDefault(true);a.setIsShippingDefault(true);
    s.save(a);assertEquals("Main",a.getLine1());assertEquals(2,s.clears);
    a.setCity(city(true));assertThrows(IllegalArgumentException.class,()->s.validate(a));
    assertFalse(hasMethod(PartyAddress.class,"getCompany"));
  }
  @Test void tagIsCompanyScopedAndLinksCannotCrossCompanies(){
    Company a=company(1L),b=company(2L);TestTagService s=new TestTagService(a,new UsableParty());
    PartyTag tag=new PartyTag();tag.setName(" VIP ");s.saveTag(tag);assertSame(a,tag.getCompany());assertEquals("VIP",tag.getName());
    Party party=party(a);assertNotNull(s.link(party,tag));
    PartyTag other=new PartyTag();other.setCompany(b);other.setName("Other");other.setActive(true);other.setArchived(false);
    assertThrows(IllegalArgumentException.class,()->s.link(party,other));
    s.duplicateLink=new PartyTagLink();assertThrows(IllegalArgumentException.class,()->s.link(party,tag));
    assertFalse(hasMethod(PartyTagLink.class,"getCompany"));
  }
  private static PartyContactPoint contact(Party p,String t,String v){PartyContactPoint c=new PartyContactPoint();c.setParty(p);c.setType(t);c.setValue(v);return c;}
  private static Party party(Company c){Party p=new Party();p.setCompany(c);p.setActive(true);p.setArchived(false);return p;}
  private static Company company(Long id){Company c=new Company();c.setId(id);c.setActive(true);c.setArchived(false);return c;}
  private static City city(boolean archived){Country c=new Country();c.setArchived(false);State s=new State();s.setCountry(c);s.setArchived(false);City city=new City();city.setState(s);city.setArchived(archived);return city;}
  private static boolean hasMethod(Class<?>c,String n){return java.util.Arrays.stream(c.getMethods()).anyMatch(m->m.getName().equals(n));}
  private static class TestContactService extends PartyContactServiceImpl{
    int locks,clears;TestContactService(){super(null,new UsableParty());}
    protected synchronized void lockParty(Party p){locks++;}protected synchronized void clearPrevious(PartyContactPoint c){clears++;}
    protected PartyContactPoint persist(PartyContactPoint c){return c;}
  }
  private static class TestAddressService extends PartyAddressServiceImpl{
    int clears;TestAddressService(){super(null,new UsableParty());}
    protected void lockParty(Party p){}protected void clearDefault(PartyAddress a,boolean b){clears++;}
    protected PartyAddress persist(PartyAddress a){return a;}
  }
  private static class TestTagService extends PartyTagServiceImpl{
    PartyTag duplicateTag;PartyTagLink duplicateLink;
    TestTagService(Company c,PartyService p){super(null,null,p,new ActiveStub(c));}
    protected PartyTag findDuplicateTag(PartyTag t){return duplicateTag;}protected PartyTagLink findDuplicateLink(PartyTagLink l){return duplicateLink;}
    protected PartyTag persistTag(PartyTag t){return t;}protected PartyTagLink persistLink(PartyTagLink l){return l;}
  }
  private static class UsableParty implements PartyService{
    public Party save(Party p){return p;}public void validate(Party p){}public void archive(Party p){}public Party restore(Party p){return p;}
    public void requireUsable(Party p){if(p==null||Boolean.TRUE.equals(p.getArchived()))throw new IllegalArgumentException();}
  }
  private static class ActiveStub implements ActiveOrganizationService{
    final Company c;ActiveStub(Company c){this.c=c;}public Optional<Company>getActiveCompany(){return Optional.of(c);}public Company requireActiveCompany(){return c;}
    public Optional<Branch>getActiveBranch(){return Optional.empty();}public Branch requireActiveBranch(){throw new UnsupportedOperationException();}
    public ActiveOrganizationContext getContext(){return null;}public Company setActiveCompany(Company c){return c;}public Company setActiveCompany(Long id){return c;}
    public Branch setActiveBranch(Branch b){return b;}public Branch setActiveBranch(Long id){return null;}public void clearActiveCompany(){}public void clearActiveBranch(){}public void clearContext(){}
    public List<Company>getAvailableCompanies(){return List.of(c);}public List<Branch>getAvailableBranches(){return List.of();}public List<Branch>getAvailableBranches(Company c){return List.of();}
  }
}
