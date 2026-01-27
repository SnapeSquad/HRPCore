package ru.hrp.economy;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface EconomyService {
    /**
     * Loads the economy account for a player from the database.
     */
    CompletableFuture<EconomyAccount> loadAccount(UUID uuid);

    /**
     * Unloads the economy account from memory.
     */
    void unloadAccount(UUID uuid);

    /**
     * Gets the current balance of a player.
     */
    BigDecimal getBalance(UUID uuid);

    /**
     * Checks if a player has an account loaded.
     */
    boolean hasAccount(UUID uuid);

    /**
     * Deposits money into a player's account.
     */
    void deposit(UUID uuid, BigDecimal amount);

    /**
     * Withdraws money from a player's account.
     * Returns true if successful, false if insufficient funds.
     */
    boolean withdraw(UUID uuid, BigDecimal amount);

    /**
     * Sets the balance of a player's account.
     */
    void setBalance(UUID uuid, BigDecimal amount);
}
