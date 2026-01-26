package ru.hrp.player;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import ru.hrp.core.DatabaseService;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class PlayerDataManager implements PlayerDataService {
    private final JavaPlugin plugin;
    private final DatabaseService databaseService;
    private final Map<UUID, RPPlayer> cache = new ConcurrentHashMap<>();

    public PlayerDataManager(JavaPlugin plugin, DatabaseService databaseService) {
        this.plugin = plugin;
        this.databaseService = databaseService;
    }

    @Override
    public CompletableFuture<RPPlayer> loadPlayerData(UUID uuid, String name) {
        return databaseService.queryAsync(connection -> {
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
                            BigDecimal.ZERO, // Placeholder
                            "NONE",           // Placeholder
                            "NONE",           // Placeholder
                            Set.of()          // Placeholder
                        );
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load player data for " + uuid, e);
            }
            return null;
        }).thenCompose(player -> {
            CompletableFuture<RPPlayer> future = new CompletableFuture<>();

            // Switch to main thread to update cache and finalize player state
            Bukkit.getScheduler().runTask(plugin, () -> {
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
                        player.economyBalance(),
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
                plugin.getLogger().log(Level.SEVERE, "Failed to save player data for " + player.uuid(), e);
            }
        });
    }

    @Override
    public Optional<RPPlayer> getPlayer(UUID uuid) {
        return Optional.ofNullable(cache.get(uuid));
    }
}
