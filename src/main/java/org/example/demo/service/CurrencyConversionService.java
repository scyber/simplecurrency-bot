package org.example.demo.service;

import org.example.demo.entity.Currency;
import org.example.demo.service.impl.HardcodedCurrencyConversionService;
import org.example.demo.service.impl.NbrbCurrencyConversionService;

public interface CurrencyConversionService {
    static CurrencyConversionService getInstance() {
        return new HardcodedCurrencyConversionService();
    }

    double getConversionRatio(Currency original, Currency target);
}
