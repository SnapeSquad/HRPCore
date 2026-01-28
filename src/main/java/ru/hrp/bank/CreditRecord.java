package ru.hrp.bank;

import java.math.BigDecimal;
import java.util.UUID;

public record CreditRecord(
    int id,
    UUID uuid,
    BigDecimal principal,
    BigDecimal interestRate,
    BigDecimal totalAmount,
    BigDecimal remainingAmount,
    long dueTimestamp,
    CreditStatus status
) {
    public boolean isDefaulted() {
        return (status == CreditStatus.ACTIVE || status == CreditStatus.DEFAULTED)
            && System.currentTimeMillis() >= dueTimestamp
            && remainingAmount.compareTo(BigDecimal.ZERO) > 0;
    }
}
