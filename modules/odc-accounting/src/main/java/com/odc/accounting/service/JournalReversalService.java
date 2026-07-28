package com.odc.accounting.service;

import com.odc.accounting.db.JournalEntry;
import java.time.LocalDate;
import java.util.Optional;

public interface JournalReversalService {
  JournalReversalResult reverse(JournalEntry originalEntry, LocalDate reversalDate, String reason);
  void validateForReversal(JournalEntry originalEntry, LocalDate reversalDate, String reason);
  Optional<JournalEntry> findReversal(JournalEntry originalEntry);
}
