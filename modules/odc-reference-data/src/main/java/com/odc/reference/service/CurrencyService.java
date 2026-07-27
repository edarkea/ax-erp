package com.odc.reference.service;

import com.odc.reference.db.Currency;

public interface CurrencyService {

  Currency save(Currency currency);

  void validate(Currency currency);
}
