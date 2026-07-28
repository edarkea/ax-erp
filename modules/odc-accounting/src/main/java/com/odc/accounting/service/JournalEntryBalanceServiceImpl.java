package com.odc.accounting.service;

import com.axelor.i18n.I18n;
import com.google.inject.Inject;
import com.odc.accounting.db.JournalEntry;
import com.odc.accounting.db.JournalLine;
import java.math.BigDecimal;
import java.util.List;

public class JournalEntryBalanceServiceImpl implements JournalEntryBalanceService {
  private final JournalLineService lineService;
  private final AccountingPeriodService periodService;
  @Inject
  public JournalEntryBalanceServiceImpl(
      JournalLineService lineService, AccountingPeriodService periodService) {
    this.lineService = lineService; this.periodService = periodService;
  }
  @Override
  public JournalEntryTotals calculateTotals(JournalEntry entry) {
    if (entry == null) throw error("Journal entry is required.");
    List<JournalLine> lines = lineService.findActiveLines(entry);
    BigDecimal debit = lines.stream().map(JournalLine::getDebit)
        .map(value -> value == null ? BigDecimal.ZERO : value).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal credit = lines.stream().map(JournalLine::getCredit)
        .map(value -> value == null ? BigDecimal.ZERO : value).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal difference = debit.subtract(credit);
    boolean positive = debit.signum() > 0 && credit.signum() > 0;
    int scale = entry.getCurrency() == null || entry.getCurrency().getDecimalPlaces() == null
        ? 2 : entry.getCurrency().getDecimalPlaces();
    return new JournalEntryTotals(debit, credit, difference, lines.size(),
        positive && difference.compareTo(BigDecimal.ZERO) == 0, positive,
        entry.getCurrency(), scale);
  }
  @Override public boolean isBalanced(JournalEntry entry) { return calculateTotals(entry).balanced(); }
  @Override
  public void requireBalanced(JournalEntry entry) {
    List<JournalLine> lines = lineService.findActiveLines(entry);
    lines.forEach(lineService::validateForPosting);
    JournalEntryTotals totals = calculateTotals(entry);
    if (totals.lineCount() < 2) throw error("Journal entry must contain at least two lines.");
    if (!totals.hasPositiveTotal()) throw error("Journal entry total must be greater than zero.");
    if (!totals.balanced()) throw error("Journal entry is not balanced.");
  }
  @Override
  public void requireReadyForPosting(JournalEntry entry) {
    if (entry == null || !"DRAFT".equals(entry.getStatus()) || Boolean.TRUE.equals(entry.getArchived()))
      throw error("Only a draft journal entry can be posted.");
    periodService.requireOpenPeriod(entry.getCompany(), entry.getAccountingDate());
    requireBalanced(entry);
  }
  private IllegalArgumentException error(String message) {
    return new IllegalArgumentException(I18n.get(message));
  }
}
