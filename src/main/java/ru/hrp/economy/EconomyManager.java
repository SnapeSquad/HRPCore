package ru.hrp.economy;

import ru.hrp.core.DatabaseService;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EconomyManager implements EconomyService {
    private final Logger logger;
    private final DatabaseService databaseService;
    private final Consumer<Runnable> syncExecutor;
    private final Map<UUID, BigDecimal> balanceCache = new ConcurrentHashMap<>();

    public EconomyManager(Logger logger, DatabaseService databaseService, Consumer<Runnable> syncExecutor) {
        this.logger = logger;
        this.databaseService = databaseService;
        this.syncExecutor = syncExecutor;
    }

    @Override
    public CompletableFuture<EconomyAccount> loadAccount(UUID uuid) {
        return databaseService.queryAsync(connection -> {
            String sql = "SELECT balance FROM economy_accounts WHERE uuid = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return new BigDecimal(rs.getString("balance"));
                    }
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to load economy account for " + uuid, e);
            }
            return BigDecimal.ZERO; // Default balance if not found
        }).thenCompose(balance -> {
            CompletableFuture<EconomyAccount> future = new CompletableFuture<>();
            syncExecutor.accept(() -> {
                balanceCache.put(uuid, balance);
                future.complete(new EconomyAccount(uuid, balance));
            });
            return future;
        });
    }

    @Override
    public void unloadAccount(UUID uuid) {
        BigDecimal balance = balanceCache.remove(uuid);
        if (balance != null) {
            saveAccount(uuid, balance);
        }
    }

    private void saveAccount(UUID uuid, BigDecimal balance) {
        databaseService.executeAsync(connection -> {
            String sql = "INSERT INTO economy_accounts (uuid, balance) VALUES (?, ?) " +
                         "ON CONFLICT(uuid) DO UPDATE SET balance = EXCLUDED.balance";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                pstmt.setString(2, balance.toPlainString());
                pstmt.executeUpdate();
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to save economy account for " + uuid, e);
            }
        });
    }

    @Override
    public BigDecimal getBalance(UUID uuid) {
        return balanceCache.getOrDefault(uuid, BigDecimal.ZERO);
    }

    @Override
    public boolean hasAccount(UUID uuid) {
        return balanceCache.containsKey(uuid);
    }

    @Override
    public void deposit(UUID uuid, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return;

        balanceCache.compute(uuid, (key, current) -> {
            BigDecimal newBalance = (current == null ? BigDecimal.ZERO : current).add(amount);
            if (amount.compareTo(new BigDecimal("10000")) >= 0) {
                logger.info("[ECONOMY] Large deposit: " + uuid + " + " + amount + " = " + newBalance);
            }
            return newBalance;
        });
    }

    @Override
    public boolean withdraw(UUID uuid, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) return false;
        if (amount.compareTo(BigDecimal.ZERO) == 0) return true;

        BigDecimal[] result = new BigDecimal[1]; // Using array to get value out of compute
        balanceCache.compute(uuid, (key, current) -> {
            BigDecimal currentBalance = (current == null ? BigDecimal.ZERO : current);
            if (currentBalance.compareTo(amount) < 0) {
                result[0] = null;
                return currentBalance;
            }
            BigDecimal newBalance = currentBalance.subtract(amount);
            result[0] = newBalance;
            if (amount.compareTo(new BigDecimal("10000")) >= 0) {
                logger.info("[ECONOMY] Large withdrawal: " + uuid + " - " + amount + " = " + newBalance);
            }
            return newBalance;
        });

        return result[0] != null;
    }

    @Override
    public void setBalance(UUID uuid, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) amount = BigDecimal.ZERO;

        BigDecimal finalAmount = amount;
        balanceCache.put(uuid, finalAmount);
        logger.info("[ECONOMY] Balance set: " + uuid + " = " + finalAmount);
    }

    @Override
    public void saveAll() {
        balanceCache.forEach((uuid, balance) -> {
            saveAccount(uuid, balance);
        });
    }
}
