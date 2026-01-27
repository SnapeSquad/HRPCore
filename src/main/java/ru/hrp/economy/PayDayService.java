package ru.hrp.economy;

import java.math.BigDecimal;
import java.util.UUID;

public interface PayDayService {
    /**
     * Processes a PayDay payout for a specific player.
     * @param uuid The player's UUID.
     * @param baseAmount The base salary amount.
     * @param multiplier The multiplier to apply.
     * @return The result of the payout.
     */
    PayDayResult processPayDay(UUID uuid, BigDecimal baseAmount, double multiplier);
}
