package ru.hrp.player;

import ru.hrp.core.DatabaseService;
import ru.hrp.economy.EconomyAccount;
import ru.hrp.economy.EconomyService;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    private final Consumer<Runnable> syncExecutor;
    private final Map<UUID, RPPlayer> cache = new ConcurrentHashMap<>();

    public PlayerDataManager(Logger logger, DatabaseService databaseService, EconomyService economyService, Consumer<Runnable> syncExecutor) {
        this.logger = logger;
        this.databaseService = databaseService;
        this.economyService = economyService;
        this.syncExecutor = syncExecutor;
    }

    @Override
    public CompletableFuture<RPPlayer> loadPlayerData(UUID uuid, String name) {
        // Explicitly coordinate with EconomyService
        CompletableFuture<EconomyAccount> econFuture = economyService.loadAccount(uuid);

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
                            "NONE",           // Placeholder
                            "NONE",           // Placeholder
                            Set.of()          // Placeholder
                        );
                    }
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to load player data for " + uuid, e);
            }
            return null;
        });

        return CompletableFuture.allOf(econFuture, dbFuture).thenCompose(v -> {
            RPPlayer player = dbFuture.join();
            CompletableFuture<RPPlayer> future = new CompletableFuture<>();

            // Switch to main thread to update cache and finalize player state
            syncExecutor.accept(() -> {
                long now = System.currentTimeMillis();
                RPPlayer finalPlayer;

                if (player == null) {
                    finalPlayer = RPPlayer.createDefault(uuid, name);
                } else {
                    // Update name and last seen
                    finalPlayer = new RPPlayer(
                        uuid,
                        name,
                        player.firstJoin(),
                        now,
                        player.role(),
                        player.job(),
                        player.talents()
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
