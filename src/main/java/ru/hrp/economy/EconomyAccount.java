package ru.hrp.economy;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Represents a player's economy account.
 */
public record EconomyAccount(
    UUID uuid,
    BigDecimal balance
) {}
