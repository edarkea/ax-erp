package com.odc.organization.context;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;

public class OrganizationContextStoreTest {
  @Test
  void shouldStoreOnlyIdsAndClearConsistently() {
    MemoryStore store = new MemoryStore();
    store.setCompanyId(1L);
    store.setBranchId(2L);
    assertEquals(Optional.of(1L), store.getCompanyId());
    assertEquals(Optional.of(2L), store.getBranchId());
    store.clearBranchId();
    assertTrue(store.getBranchId().isEmpty());
    assertEquals(Optional.of(1L), store.getCompanyId());
    store.clear();
    assertTrue(store.getCompanyId().isEmpty());
  }

  @Test
  void shouldBeIsolatedByStoreInstance() {
    MemoryStore first = new MemoryStore();
    MemoryStore second = new MemoryStore();
    first.setCompanyId(1L);
    assertTrue(second.getCompanyId().isEmpty());
  }

  public static class MemoryStore implements OrganizationContextStore {
    Long company; Long branch;
    public Optional<Long> getCompanyId() { return Optional.ofNullable(company); }
    public void setCompanyId(Long id) { company = id; }
    public void clearCompanyId() { company = null; }
    public Optional<Long> getBranchId() { return Optional.ofNullable(branch); }
    public void setBranchId(Long id) { branch = id; }
    public void clearBranchId() { branch = null; }
    public void clear() { company = null; branch = null; }
  }
}
