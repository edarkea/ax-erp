package com.odc.accounting.service;

import com.axelor.auth.db.User;
import com.odc.accounting.db.JournalEntry;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountingPostingResult(
    JournalEntry journalEntry, String entryNumber, Integer postingYear,
    Long postingSequence, LocalDateTime postedAt, User postedBy,
    BigDecimal totalDebit, BigDecimal totalCredit, int lineCount) {}
