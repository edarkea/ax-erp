package com.odc.reference.service;

import com.axelor.db.Query;
import com.axelor.i18n.I18n;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import com.odc.reference.db.Currency;
import com.odc.reference.db.repo.CurrencyRepository;

import java.util.Locale;

public class CurrencyServiceImpl implements CurrencyService {

    private final CurrencyRepository currencyRepository;

    @Inject
    public CurrencyServiceImpl(CurrencyRepository currencyRepository) {
        this.currencyRepository = currencyRepository;
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public Currency save(Currency currency) {
        validate(currency);

        Currency archivedCurrency = findOtherByCode(currency.getCode(), true, currency.getId());
        if (archivedCurrency != null) {
            archivedCurrency.setArchived(false);
            archivedCurrency.setName(currency.getName());
            archivedCurrency.setSymbol(currency.getSymbol());
            archivedCurrency.setDecimalPlaces(currency.getDecimalPlaces());
            return persist(archivedCurrency);
        }

        return persist(currency);
    }

    @Override
    public void validate(Currency currency) {
        if (currency == null) {
            throw inconsistency("Currency is required.");
        }
        if (currency.getArchived() == null) {
            currency.setArchived(false);
        }

        String normalizedCode =
                currency.getCode() == null ? null : currency.getCode().trim().toUpperCase(Locale.ROOT);
        currency.setCode(normalizedCode);

        if (normalizedCode == null || normalizedCode.isEmpty()) {
            throw inconsistency("Currency code is required.");
        }
        if (normalizedCode.length() != 3) {
            throw inconsistency("Currency code must contain exactly three characters.");
        }

        Integer decimalPlaces = currency.getDecimalPlaces();
        if (decimalPlaces == null) {
            currency.setDecimalPlaces(2);
        } else if (decimalPlaces < 0 || decimalPlaces > 6) {
            throw inconsistency("Currency decimal places must be between 0 and 6.");
        }

        Currency duplicate = findOtherByCode(normalizedCode, false, currency.getId());
        if (duplicate != null) {
            throw inconsistency("An active currency with code {0} already exists.", normalizedCode);
        }
    }

    protected Currency findOtherByCode(String code, boolean archived, Long excludedId) {
        if (excludedId == null) {
            return currencyRepository
                    .all()
                    .filter("self.code = :code AND self.archived = :archived")
                    .bind("code", code)
                    .bind("archived", archived)
                    .fetchOne();
        }

        return currencyRepository
                .all()
                .filter(
                        "self.code = :code "
                                + "AND self.archived = :archived "
                                + "AND self.id != :excludedId")
                .bind("code", code)
                .bind("archived", archived)
                .bind("excludedId", excludedId)
                .fetchOne();
    }

    protected Currency persist(Currency currency) {
        return currencyRepository.save(currency);
    }

    private IllegalArgumentException inconsistency(String message, Object... args) {
        return new IllegalArgumentException(String.format(I18n.get(message).replace("{0}", "%s"), args));
    }
}
