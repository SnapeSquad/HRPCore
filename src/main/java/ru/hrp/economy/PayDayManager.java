package ru.hrp.economy;

import java.math.BigDecimal;
import java.util.UUID;

public class PayDayManager implements PayDayService {
    private final EconomyService economyService;

    public PayDayManager(EconomyService economyService) {
        this.economyService = economyService;
    }

    @Override
    public PayDayResult processPayDay(UUID uuid, BigDecimal baseAmount, double multiplier) {
        if (!economyService.hasAccount(uuid)) {
            return new PayDayResult(uuid, BigDecimal.ZERO, multiplier, false);
        }

        BigDecimal finalAmount = baseAmount.multiply(BigDecimal.valueOf(multiplier));
        economyService.deposit(uuid, finalAmount);

        return new PayDayResult(uuid, finalAmount, multiplier, true);
    }
}
