package com.odc.accounting.service;

import com.odc.accounting.db.JournalEntry;
import com.odc.organization.db.Company;
import java.util.Optional;

public interface JournalEntryService {
  JournalEntry save(JournalEntry entry);
  void validate(JournalEntry entry);
  JournalEntry resolvePeriod(JournalEntry entry);
  JournalEntry cancel(JournalEntry entry, String reason);
  void archive(JournalEntry entry);
  JournalEntry restore(JournalEntry entry);
  void requireEditable(JournalEntry entry);
  void requireUsable(JournalEntry entry);
  Optional<JournalEntry> findBySource(Company company, String sourceModel, Long sourceRecordId);
}
