package ru.hrp.bank;

import ru.hrp.core.DatabaseService;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BankManager implements BankService {
    private final Logger logger;
    private final DatabaseService databaseService;
    private final Consumer<Runnable> syncExecutor;
    private final Map<UUID, BigDecimal> bankCache = new ConcurrentHashMap<>();
    private final Map<UUID, List<CreditRecord>> creditCache = new ConcurrentHashMap<>();
    private final List<CreditSanctionHook> sanctionHooks = new ArrayList<>();

    public BankManager(Logger logger, DatabaseService databaseService, Consumer<Runnable> syncExecutor) {
        this.logger = logger;
        this.databaseService = databaseService;
        this.syncExecutor = syncExecutor;
    }

    @Override
    public CompletableFuture<BigDecimal> loadAccount(UUID uuid) {
        return databaseService.queryAsync(connection -> {
            String sql = "SELECT balance FROM bank_accounts WHERE uuid = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return new BigDecimal(rs.getString("balance"));
                    }
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to load bank account for " + uuid, e);
            }
            return BigDecimal.ZERO;
        }).thenCompose(balance -> {
            CompletableFuture<BigDecimal> future = new CompletableFuture<>();
            syncExecutor.accept(() -> {
                bankCache.put(uuid, balance);
                future.complete(balance);
            });
            return future;
        });
    }

    @Override
    public void unloadAccount(UUID uuid) {
        BigDecimal balance = bankCache.remove(uuid);
        if (balance != null) {
            saveAccount(uuid, balance);
        }
    }

    private void saveAccount(UUID uuid, BigDecimal balance) {
        databaseService.executeAsync(connection -> {
            String sql = "INSERT INTO bank_accounts (uuid, balance) VALUES (?, ?) " +
                         "ON CONFLICT(uuid) DO UPDATE SET balance = EXCLUDED.balance";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                pstmt.setString(2, balance.toPlainString());
                pstmt.executeUpdate();
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to save bank account for " + uuid, e);
            }
        });
    }

    @Override
    public CompletableFuture<List<CreditRecord>> loadCredits(UUID uuid) {
        return databaseService.queryAsync(connection -> {
            List<CreditRecord> credits = new ArrayList<>();
            String sql = "SELECT * FROM player_credits WHERE uuid = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        credits.add(new CreditRecord(
                            rs.getInt("id"),
                            uuid,
                            new BigDecimal(rs.getString("principal")),
                            new BigDecimal(rs.getString("interest_rate")),
                            new BigDecimal(rs.getString("total_amount")),
                            new BigDecimal(rs.getString("remaining_amount")),
                            rs.getLong("due_timestamp"),
                            CreditStatus.valueOf(rs.getString("status"))
                        ));
                    }
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to load credits for " + uuid, e);
            }
            return credits;
        }).thenCompose(credits -> {
            CompletableFuture<List<CreditRecord>> future = new CompletableFuture<>();
            syncExecutor.accept(() -> {
                List<CreditRecord> synched = Collections.synchronizedList(new ArrayList<>(credits));
                creditCache.put(uuid, synched);

                // Check for defaults on load
                for (int i = 0; i < synched.size(); i++) {
                    CreditRecord credit = synched.get(i);
                    if (credit.isDefaulted() && credit.status() == CreditStatus.ACTIVE) {
                        CreditRecord defaulted = new CreditRecord(
                            credit.id(), credit.uuid(), credit.principal(), credit.interestRate(),
                            credit.totalAmount(), credit.remainingAmount(), credit.dueTimestamp(), CreditStatus.DEFAULTED
                        );
                        synched.set(i, defaulted);
                        saveCredit(defaulted);
                        triggerSanctions(uuid, defaulted);
                    }
                }
                future.complete(synched);
            });
            return future;
        });
    }

    @Override
    public void unloadCredits(UUID uuid) {
        creditCache.remove(uuid);
    }

    @Override
    public BigDecimal getBankBalance(UUID uuid) {
        return bankCache.getOrDefault(uuid, BigDecimal.ZERO);
    }

    @Override
    public void depositToBank(UUID uuid, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return;
        bankCache.compute(uuid, (k, v) -> (v == null ? BigDecimal.ZERO : v).add(amount));
    }

    @Override
    public boolean withdrawFromBank(UUID uuid, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return false;

        BigDecimal[] result = new BigDecimal[1];
        bankCache.compute(uuid, (k, v) -> {
            BigDecimal current = (v == null ? BigDecimal.ZERO : v);
            if (current.compareTo(amount) < 0) {
                result[0] = null;
                return current;
            }
            BigDecimal newVal = current.subtract(amount);
            result[0] = newVal;
            return newVal;
        });
        return result[0] != null;
    }

    @Override
    public CompletableFuture<CreditRecord> createCredit(UUID uuid, BigDecimal principal, BigDecimal interestRate, long durationMillis) {
        BigDecimal interest = principal.multiply(interestRate);
        BigDecimal total = principal.add(interest);
        long due = System.currentTimeMillis() + durationMillis;

        return databaseService.queryAsync(connection -> {
            String sql = "INSERT INTO player_credits (uuid, principal, interest_rate, total_amount, remaining_amount, due_timestamp, status) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, uuid.toString());
                pstmt.setString(2, principal.toPlainString());
                pstmt.setString(3, interestRate.toPlainString());
                pstmt.setString(4, total.toPlainString());
                pstmt.setString(5, total.toPlainString());
                pstmt.setLong(6, due);
                pstmt.setString(7, CreditStatus.ACTIVE.name());
                pstmt.executeUpdate();

                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return new CreditRecord(rs.getInt(1), uuid, principal, interestRate, total, total, due, CreditStatus.ACTIVE);
                    }
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to create credit for " + uuid, e);
            }
            return null;
        }).thenCompose(record -> {
            CompletableFuture<CreditRecord> future = new CompletableFuture<>();
            syncExecutor.accept(() -> {
                if (record != null) {
                    creditCache.computeIfAbsent(uuid, k -> Collections.synchronizedList(new ArrayList<>())).add(record);
                }
                future.complete(record);
            });
            return future;
        });
    }

    @Override
    public CompletableFuture<Void> repayCredit(int creditId, BigDecimal amount) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        syncExecutor.accept(() -> {
            // Find in cache
            CreditRecord found = null;
            UUID ownerUuid = null;
            int foundIdx = -1;

            for (var entry : creditCache.entrySet()) {
                List<CreditRecord> list = entry.getValue();
                synchronized (list) {
                    for (int i = 0; i < list.size(); i++) {
                        if (list.get(i).id() == creditId) {
                            found = list.get(i);
                            ownerUuid = entry.getKey();
                            foundIdx = i;
                            break;
                        }
                    }
                }
                if (found != null) break;
            }

            if (found == null || found.status() == CreditStatus.PAID) {
                future.complete(null);
                return;
            }

            BigDecimal newRemaining = found.remainingAmount().subtract(amount);
            if (newRemaining.compareTo(BigDecimal.ZERO) < 0) newRemaining = BigDecimal.ZERO;

            CreditStatus newStatus = found.status();
            if (newRemaining.compareTo(BigDecimal.ZERO) == 0) {
                newStatus = CreditStatus.PAID;
            } else if (System.currentTimeMillis() >= found.dueTimestamp()) {
                newStatus = CreditStatus.DEFAULTED;
            }

            CreditRecord updated = new CreditRecord(
                found.id(), found.uuid(), found.principal(), found.interestRate(),
                found.totalAmount(), newRemaining, found.dueTimestamp(), newStatus
            );

            // Update cache
            if (ownerUuid != null) {
                List<CreditRecord> list = creditCache.get(ownerUuid);
                if (list != null) list.set(foundIdx, updated);
            }

            // Save to DB
            saveCredit(updated);

            if (newStatus == CreditStatus.DEFAULTED && found.status() != CreditStatus.DEFAULTED) {
                triggerSanctions(ownerUuid, updated);
            }
            future.complete(null);
        });
        return future;
    }

    private void saveCredit(CreditRecord credit) {
        databaseService.executeAsync(connection -> {
            String sql = "UPDATE player_credits SET remaining_amount = ?, status = ? WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, credit.remainingAmount().toPlainString());
                pstmt.setString(2, credit.status().name());
                pstmt.setInt(3, credit.id());
                pstmt.executeUpdate();
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to update credit " + credit.id(), e);
            }
        });
    }

    @Override
    public void registerSanctionHook(CreditSanctionHook hook) {
        sanctionHooks.add(hook);
    }

    private void triggerSanctions(UUID uuid, CreditRecord credit) {
        sanctionHooks.forEach(hook -> {
            try {
                hook.onCreditDefault(uuid, credit);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error in credit sanction hook", e);
            }
        });
    }

    @Override
    public void saveAll() {
        bankCache.forEach(this::saveAccount);
        // Credits are saved immediately on update in this implementation
    }
}
