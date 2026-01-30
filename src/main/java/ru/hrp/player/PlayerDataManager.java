package ru.hrp.player;

import ru.hrp.core.DatabaseService;
import ru.hrp.economy.EconomyAccount;

import java.math.BigDecimal;
import ru.hrp.bank.BankService;
import ru.hrp.crime.CrimeRecord;
import ru.hrp.crime.CrimeService;
import ru.hrp.jail.JailService;
import ru.hrp.jail.JailState;
import ru.hrp.government.FactionId;
import ru.hrp.government.FactionService;
import ru.hrp.jobs.JobService;
import ru.hrp.medical.MedicalData;
import ru.hrp.medical.MedicalService;
import ru.hrp.economy.EconomyService;
import ru.hrp.roles.RoleId;
import ru.hrp.roles.RoleService;
import ru.hrp.talents.TalentId;
import ru.hrp.talents.TalentService;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PlayerDataManager implements PlayerDataService {
    private final Logger logger;
    private final DatabaseService databaseService;
    private final EconomyService economyService;
    private final RoleService roleService;
    private final TalentService talentService;
    private final CrimeService crimeService;
    private final JailService jailService;
    private final BankService bankService;
    private final MedicalService medicalService;
    private final JobService jobService;
    private final FactionService factionService;
    private final Consumer<Runnable> syncExecutor;
    private final Map<UUID, RPPlayer> cache = new ConcurrentHashMap<>();

    public PlayerDataManager(Logger logger, DatabaseService databaseService, EconomyService economyService, RoleService roleService, TalentService talentService, CrimeService crimeService, JailService jailService, BankService bankService, MedicalService medicalService, JobService jobService, FactionService factionService, Consumer<Runnable> syncExecutor) {
        this.logger = logger;
        this.databaseService = databaseService;
        this.economyService = economyService;
        this.roleService = roleService;
        this.talentService = talentService;
        this.crimeService = crimeService;
        this.jailService = jailService;
        this.bankService = bankService;
        this.medicalService = medicalService;
        this.jobService = jobService;
        this.factionService = factionService;
        this.syncExecutor = syncExecutor;
    }

    @Override
    public CompletableFuture<RPPlayer> loadPlayerData(UUID uuid, String name) {
        // Explicitly coordinate with services
        CompletableFuture<EconomyAccount> econFuture = economyService.loadAccount(uuid);
        CompletableFuture<RoleId> roleFuture = roleService.loadRole(uuid);
        CompletableFuture<Map<TalentId, Integer>> talentFuture = talentService.loadTalents(uuid);
        CompletableFuture<List<CrimeRecord>> crimeFuture = crimeService.loadPlayerCrimes(uuid);
        CompletableFuture<JailState> jailFuture = jailService.loadJailState(uuid);
        CompletableFuture<BigDecimal> bankFuture = bankService.loadAccount(uuid);
        CompletableFuture<List<ru.hrp.bank.CreditRecord>> creditsFuture = bankService.loadCredits(uuid);
        CompletableFuture<MedicalData> medicalFuture = medicalService.loadMedicalData(uuid);
        CompletableFuture<ru.hrp.jobs.JobId> jobFuture = jobService.loadJob(uuid);
        CompletableFuture<FactionId> factionFuture = factionService.loadFaction(uuid);
        CompletableFuture<String> rankFuture = factionService.loadRank(uuid);

        CompletableFuture<RPPlayer> dbFuture = databaseService.queryAsync(connection -> {
            String sql = "SELECT * FROM players WHERE uuid = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return new RPPlayer(
                            uuid,
                            rs.getString("last_name"),
                            rs.getLong("first_join"),
                            rs.getLong("last_seen"),
                            RoleId.NONE,      // Placeholder
                            Collections.emptyMap() // Placeholder
                        );
                    }
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to load player data for " + uuid, e);
            }
            return null;
        });

        return CompletableFuture.allOf(econFuture, roleFuture, talentFuture, crimeFuture, jailFuture, bankFuture, creditsFuture, medicalFuture, jobFuture, factionFuture, rankFuture, dbFuture).thenCompose(v -> {
            RPPlayer player = dbFuture.join();
            CompletableFuture<RPPlayer> future = new CompletableFuture<>();

            // Switch to main thread to update cache and finalize player state
            syncExecutor.accept(() -> {
                long now = System.currentTimeMillis();
                RPPlayer finalPlayer;

                if (player == null) {
                    finalPlayer = new RPPlayer(
                        uuid,
                        name,
                        now,
                        now,
                        roleFuture.join(),
                        talentFuture.join()
                    );
                } else {
                    // Update name and last seen
                    finalPlayer = new RPPlayer(
                        uuid,
                        name,
                        player.firstJoin(),
                        now,
                        roleFuture.join(),
                        talentFuture.join()
                    );
                }

                cache.put(uuid, finalPlayer);

                // Trigger an async save to update name/last_seen in DB
                savePlayerData(finalPlayer);

                future.complete(finalPlayer);
            });

            return future;
        });
    }

    @Override
    public void unloadPlayerData(UUID uuid) {
        RPPlayer player = cache.remove(uuid);
        if (player != null) {
            savePlayerData(player);
        }
        economyService.unloadAccount(uuid);
        roleService.unloadRole(uuid);
        talentService.unloadTalents(uuid);
        crimeService.unloadPlayerCrimes(uuid);
        jailService.unloadJailState(uuid);
        bankService.unloadAccount(uuid);
        bankService.unloadCredits(uuid);
        medicalService.unloadMedicalData(uuid);
        jobService.unloadJob(uuid);
        factionService.unloadFaction(uuid);
    }

    @Override
    public CompletableFuture<Void> savePlayerData(RPPlayer player) {
        return databaseService.executeAsync(connection -> {
            String sql = "INSERT INTO players (uuid, last_name, first_join, last_seen) VALUES (?, ?, ?, ?) " +
                         "ON CONFLICT(uuid) DO UPDATE SET last_name = EXCLUDED.last_name, last_seen = EXCLUDED.last_seen";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, player.uuid().toString());
                pstmt.setString(2, player.lastKnownName());
                pstmt.setLong(3, player.firstJoin());
                pstmt.setLong(4, player.lastSeen());
                pstmt.executeUpdate();
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to save player data for " + player.uuid(), e);
            }
        });
    }

    @Override
    public Optional<RPPlayer> getPlayer(UUID uuid) {
        return Optional.ofNullable(cache.get(uuid));
    }

    @Override
    public void saveAll() {
        for (RPPlayer player : cache.values()) {
            savePlayerData(player).join(); // Use join() to ensure it's submitted and processed if possible
        }
    }
}
