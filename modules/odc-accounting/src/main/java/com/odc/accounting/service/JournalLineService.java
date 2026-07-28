package com.odc.accounting.service;

import com.odc.accounting.db.JournalEntry;
import com.odc.accounting.db.JournalLine;
import java.util.List;

public interface JournalLineService {
  JournalLine save(JournalLine line);
  void validate(JournalLine line);
  void validateForPosting(JournalLine line);
  void archive(JournalLine line);
  JournalLine restore(JournalLine line);
  void requireEditable(JournalLine line);
  List<JournalLine> findActiveLines(JournalEntry entry);
  List<JournalLine> lockActiveLines(JournalEntry entry);
}
