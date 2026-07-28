package com.odc.party.service;

import static org.junit.jupiter.api.Assertions.*;

import com.odc.organization.context.ActiveOrganizationContext;
import com.odc.organization.db.Branch;
import com.odc.organization.db.Company;
import com.odc.organization.service.ActiveOrganizationService;
import com.odc.party.db.Party;
import com.odc.party.db.PartyRole;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PartyAndRoleServiceTest {
  @Test
  void createsPartyInActiveCompanyAndNormalizesIdentification() {
    Company active = company(1L);
    TestPartyService service = new TestPartyService(active);
    Party party = party(null, "  Acme  ", " ab 12 ");
    service.save(party);
    assertSame(active, party.getCompany());
    assertEquals("Acme", party.getDisplayName());
    assertEquals("AB12", party.getTaxId());
  }

  @Test
  void rejectsManipulatedCompanyDuplicateAndMissingIdentificationType() {
    Company active = company(1L);
    TestPartyService service = new TestPartyService(active);
    Party existing = party(company(2L), "Other", "X");
    existing.setId(5L);
    assertThrows(IllegalArgumentException.class, () -> service.validate(existing));

    Party duplicate = party(active, "Duplicate", "X");
    service.duplicate = duplicate;
    assertThrows(IllegalArgumentException.class, () -> service.validate(duplicate));

    Party missingType = party(active, "Missing", "X");
    missingType.setTaxIdentificationType(null);
    assertThrows(IllegalArgumentException.class, () -> service.validate(missingType));
  }

  @Test
  void allowsNoIdAndSameIdInDifferentCompanies() {
    TestPartyService service = new TestPartyService(company(1L));
    Party withoutId = party(company(1L), "Walk in", null);
    service.validate(withoutId);
    assertNull(withoutId.getTaxId());
  }

  @Test
  void archivesAndRestoresWithUniquenessValidation() {
    TestPartyService service = new TestPartyService(company(1L));
    Party party = party(company(1L), "Acme", "X");
    party.setId(1L);
    service.archive(party);
    assertTrue(party.getArchived());
    service.duplicate = party(company(1L), "Other", "X");
    assertThrows(IllegalArgumentException.class, () -> service.restore(party));
  }

  @Test
  void assignsMultipleRoleTypesAndRejectsDuplicateOrArchivedParty() {
    Party party = party(company(1L), "Acme", null);
    party.setArchived(false);
    TestRoleService roles = new TestRoleService(new UsablePartyService());
    assertEquals("CUSTOMER", roles.assignRole(party, "CUSTOMER").getRoleType());
    assertEquals("SUPPLIER", roles.assignRole(party, "SUPPLIER").getRoleType());
    roles.duplicate = new PartyRole();
    assertThrows(IllegalArgumentException.class, () -> roles.assignRole(party, "CUSTOMER"));
    party.setArchived(true);
    assertThrows(IllegalArgumentException.class, () -> roles.assignRole(party, "OTHER"));
  }

  @Test
  void roleHasNoCompanyAndPartyHasNoUpperModuleCollections() {
    assertFalse(hasMethod(PartyRole.class, "getCompany"));
    assertFalse(hasMethod(Party.class, "getSalesInvoices"));
    assertFalse(hasMethod(Party.class, "getJournalLines"));
  }

  private static boolean hasMethod(Class<?> type, String name) {
    return java.util.Arrays.stream(type.getMethods()).anyMatch(m -> m.getName().equals(name));
  }

  private static Party party(Company company, String name, String id) {
    Party party = new Party();
    party.setCompany(company);
    party.setPartyType("ORGANIZATION");
    party.setDisplayName(name);
    party.setTaxIdentificationType(id == null ? null : "TAX_ID");
    party.setTaxId(id);
    party.setActive(true);
    party.setArchived(false);
    return party;
  }

  private static Company company(Long id) {
    Company company = new Company();
    company.setId(id);
    company.setActive(true);
    company.setArchived(false);
    return company;
  }

  private static class TestPartyService extends PartyServiceImpl {
    Party duplicate;
    TestPartyService(Company active) { super(null, new ActiveOrgStub(active)); }
    @Override protected Party persist(Party party) { return party; }
    @Override protected Party findDuplicate(Party party) { return duplicate; }
    @Override protected Company findPersistedCompany(Long id) { return null; }
  }

  private static class TestRoleService extends PartyRoleServiceImpl {
    PartyRole duplicate;
    TestRoleService(PartyService partyService) { super(null, partyService); }
    @Override protected PartyRole persist(PartyRole role) { return role; }
    @Override protected PartyRole findDuplicate(PartyRole role) { return duplicate; }
  }

  private static class UsablePartyService implements PartyService {
    public Party save(Party party) { return party; }
    public void validate(Party party) {}
    public void archive(Party party) {}
    public Party restore(Party party) { return party; }
    public void requireUsable(Party party) {
      if (party == null || Boolean.TRUE.equals(party.getArchived())) throw new IllegalArgumentException();
    }
  }

  private static class ActiveOrgStub implements ActiveOrganizationService {
    private final Company company;
    ActiveOrgStub(Company company) { this.company = company; }
    public Optional<Company> getActiveCompany() { return Optional.ofNullable(company); }
    public Company requireActiveCompany() {
      if (company == null) throw new IllegalArgumentException();
      return company;
    }
    public Optional<Branch> getActiveBranch() { return Optional.empty(); }
    public Branch requireActiveBranch() { throw new UnsupportedOperationException(); }
    public ActiveOrganizationContext getContext() { return null; }
    public Company setActiveCompany(Company value) { throw new UnsupportedOperationException(); }
    public Company setActiveCompany(Long id) { throw new UnsupportedOperationException(); }
    public Branch setActiveBranch(Branch value) { throw new UnsupportedOperationException(); }
    public Branch setActiveBranch(Long id) { throw new UnsupportedOperationException(); }
    public void clearActiveCompany() {}
    public void clearActiveBranch() {}
    public void clearContext() {}
    public List<Company> getAvailableCompanies() { return List.of(company); }
    public List<Branch> getAvailableBranches() { return List.of(); }
    public List<Branch> getAvailableBranches(Company value) { return List.of(); }
  }
}
