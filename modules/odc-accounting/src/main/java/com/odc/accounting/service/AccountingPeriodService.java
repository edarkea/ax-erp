package com.odc.accounting.service;

import com.odc.accounting.db.AccountingPeriod;
import com.odc.organization.db.Company;
import java.time.LocalDate;
import java.util.Optional;

public interface AccountingPeriodService {
  AccountingPeriod save(AccountingPeriod period);
  void validate(AccountingPeriod period);
  AccountingPeriod open(AccountingPeriod period);
  AccountingPeriod close(AccountingPeriod period);
  AccountingPeriod reopen(AccountingPeriod period);
  void archive(AccountingPeriod period);
  AccountingPeriod restore(AccountingPeriod period);
  Optional<AccountingPeriod> findPeriod(Company company, LocalDate accountingDate);
  AccountingPeriod requirePeriod(Company company, LocalDate accountingDate);
  AccountingPeriod requireOpenPeriod(Company company, LocalDate accountingDate);
  AccountingPeriod requireOpenPeriod(LocalDate accountingDate);
  boolean isDateOpen(Company company, LocalDate accountingDate);
}
