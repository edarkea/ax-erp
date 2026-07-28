package com.odc.accounting.service;

import com.axelor.auth.db.User;
import com.odc.accounting.db.JournalEntry;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record JournalReversalResult(
    JournalEntry originalEntry, JournalEntry reversalEntry,
    String originalEntryNumber, String reversalEntryNumber,
    LocalDate reversalDate, String reversalReason,
    LocalDateTime reversedAt, User reversedBy) {}
