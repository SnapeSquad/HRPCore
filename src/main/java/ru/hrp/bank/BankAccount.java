package ru.hrp.bank;

import java.math.BigDecimal;
import java.util.UUID;

public record BankAccount(
    UUID uuid,
    BigDecimal balance
) {}
