package ru.hrp.economy;

import java.math.BigDecimal;
import java.util.UUID;

public record PayDayResult(
    UUID uuid,
    BigDecimal amount,
    double multiplier,
    boolean success
) {}
