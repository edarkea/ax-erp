package com.odc.accounting.service;

import static com.odc.accounting.service.AccountingTestSupport.role;
import static org.junit.jupiter.api.Assertions.*;

import com.odc.accounting.db.AccountingRoleDefinition;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AccountingRoleDefinitionServiceImplTest {
  private TestService service;

  @BeforeEach
  void setUp() {
    service = new TestService();
  }

  @Test
  void shouldCreateNormalizeAndRemainGlobal() {
    AccountingRoleDefinition value = role(1, " receivable ", "SALES", "DEBIT");
    value.setId(null); value.setName(" Receivable ");
    service.validate(value);
    assertEquals("RECEIVABLE", value.getCode());
    assertEquals("Receivable", value.getName());
    assertFalse(Arrays.stream(AccountingRoleDefinition.class.getMethods())
        .anyMatch(method -> method.getName().equals("getCompany")));
  }

  @Test
  void shouldRejectDuplicateInvalidGroupTypeAndSide() {
    AccountingRoleDefinition value = role(1, "A", "SALES", "DEBIT");
    service.duplicate = role(2, "A", "SALES", "DEBIT");
    assertThrows(IllegalArgumentException.class, () -> service.validate(value));
    service.duplicate = null; value.setDocumentType("GENERAL_ENTRY");
    assertThrows(IllegalArgumentException.class, () -> service.validate(value));
    value.setDocumentType(null); value.setSideHint("INVALID");
    assertThrows(IllegalArgumentException.class, () -> service.validate(value));
  }

  @Test
  void shouldProtectSystemDefinitionAndArchiveCustomDefinition() {
    AccountingRoleDefinition value = role(1, "SYSTEM", "SALES", "DEBIT");
    value.setSystemDefined(true);
    service.persisted = role(1, "ORIGINAL", "SALES", "DEBIT");
    service.persisted.setSystemDefined(true);
    assertThrows(IllegalArgumentException.class, () -> service.validate(value));
    assertThrows(IllegalArgumentException.class, () -> service.archive(value));
    value.setSystemDefined(false); service.persisted = null;
    service.archive(value);
    assertTrue(value.getArchived()); assertFalse(value.getActive());
  }

  @Test
  void shouldRejectRestoreConflict() {
    AccountingRoleDefinition value = role(1, "CUSTOM", "GENERAL", "EITHER");
    value.setArchived(true); value.setActive(false);
    service.duplicate = role(2, "CUSTOM", "GENERAL", "EITHER");
    assertThrows(IllegalArgumentException.class, () -> service.restore(value));
  }

  @Test
  void shouldProvideStableInitialDefinitionsWithoutDuplicates() {
    SeedProbe seed = new SeedProbe();
    List<AccountingRoleDefinition> first = seed.values();
    List<AccountingRoleDefinition> second = seed.values();
    assertEquals(
        List.of("ACCOUNT_RECEIVABLE", "SALES_REVENUE", "OUTPUT_TAX", "SALES_DISCOUNT", "CASH"),
        first.stream().map(AccountingRoleDefinition::getCode).toList());
    assertEquals(first.stream().map(AccountingRoleDefinition::getCode).toList(),
        second.stream().map(AccountingRoleDefinition::getCode).toList());
    assertTrue(first.stream().allMatch(AccountingRoleDefinition::getSystemDefined));
    seed.seed();
    seed.seed();
    assertEquals(5, seed.persisted.size());
  }

  private static class TestService extends AccountingRoleDefinitionServiceImpl {
    AccountingRoleDefinition duplicate;
    AccountingRoleDefinition persisted;
    boolean configured;
    TestService() { super(null, null, new AccountingConfigurationValidationServiceImpl()); }
    @Override protected AccountingRoleDefinition findDuplicate(AccountingRoleDefinition value) {
      return duplicate;
    }
    @Override protected boolean hasActiveSetupEntries(AccountingRoleDefinition value) {
      return configured;
    }
    @Override protected AccountingRoleDefinition findPersisted(Long id) { return persisted; }
    @Override protected AccountingRoleDefinition persist(AccountingRoleDefinition value) {
      return value;
    }
  }
  private static class SeedProbe extends AccountingRoleDefinitionSeedServiceImpl {
    final Map<String, AccountingRoleDefinition> persisted = new HashMap<>();
    SeedProbe() { super(null, null); }
    List<AccountingRoleDefinition> values() { return definitions(); }
    @Override protected AccountingRoleDefinition findByCode(String code) {
      return persisted.get(code);
    }
    @Override protected AccountingRoleDefinition save(AccountingRoleDefinition value) {
      persisted.put(value.getCode(), value);
      return value;
    }
  }
}
