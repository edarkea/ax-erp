package com.odc.reference.service;

import com.odc.reference.db.Currency;

public interface CurrencyService {

  Currency save(Currency currency);

  Currency archive(Currency currency);

  void validate(Currency currency);
}
