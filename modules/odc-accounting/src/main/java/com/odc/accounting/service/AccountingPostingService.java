package com.odc.accounting.service;

import com.odc.accounting.db.JournalEntry;

public interface AccountingPostingService {
  AccountingPostingResult post(JournalEntry journalEntry);
  void validateForPosting(JournalEntry journalEntry);
  boolean isReadyForPosting(JournalEntry journalEntry);
}
