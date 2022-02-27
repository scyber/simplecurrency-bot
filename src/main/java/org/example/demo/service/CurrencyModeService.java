package org.example.demo.service;

import org.example.demo.entity.Currency;
import org.example.demo.service.impl.HashMapCurrencyModeService;

public interface CurrencyModeService {
    static CurrencyModeService getInstance(){
        return new HashMapCurrencyModeService();
    }
    Currency getOriginalCurrency( long chatId);
    Currency getTargetCurrency(long chatId);

    void setOriginalCurrency(long chatId, Currency currency);
    void setTargetCurrency(long chatId, Currency currency);

}
