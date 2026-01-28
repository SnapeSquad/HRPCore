package ru.hrp.bank;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface BankService {
    CompletableFuture<BigDecimal> loadAccount(UUID uuid);
    void unloadAccount(UUID uuid);

    CompletableFuture<List<CreditRecord>> loadCredits(UUID uuid);
    void unloadCredits(UUID uuid);

    BigDecimal getBankBalance(UUID uuid);
    List<CreditRecord> getCredits(UUID uuid);
    void depositToBank(UUID uuid, BigDecimal amount);
    boolean withdrawFromBank(UUID uuid, BigDecimal amount);

    CompletableFuture<CreditRecord> createCredit(UUID uuid, BigDecimal principal, BigDecimal interestRate, long durationMillis);
    CompletableFuture<Void> repayCredit(int creditId, BigDecimal amount);

    void registerSanctionHook(CreditSanctionHook hook);

    void saveAll();
}
