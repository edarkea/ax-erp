package com.odc.accounting.service;

import com.odc.accounting.db.ChartAccount;

public interface ChartAccountService {
  ChartAccount save(ChartAccount account);
  void validate(ChartAccount account);
  void validateHierarchy(ChartAccount account);
  void archive(ChartAccount account);
  ChartAccount restore(ChartAccount account);
  void requireUsable(ChartAccount account);
  void requirePostingAccount(ChartAccount account);
}
