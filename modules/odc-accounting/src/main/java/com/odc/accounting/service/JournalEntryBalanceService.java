package com.odc.accounting.service;

import com.odc.accounting.db.JournalEntry;

public interface JournalEntryBalanceService {
  JournalEntryTotals calculateTotals(JournalEntry entry);
  boolean isBalanced(JournalEntry entry);
  void requireBalanced(JournalEntry entry);
  void requireReadyForPosting(JournalEntry entry);
}
