package ru.hrp.government;

import ru.hrp.core.DatabaseService;

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

public class FactionManager implements FactionService {
    private final Logger logger;
    private final DatabaseService databaseService;
    private final Consumer<Runnable> syncExecutor;

    private final Map<UUID, FactionId> factionCache = new ConcurrentHashMap<>();
    private final Map<UUID, String> rankCache = new ConcurrentHashMap<>();

    public FactionManager(Logger logger, DatabaseService databaseService, Consumer<Runnable> syncExecutor) {
        this.logger = logger;
        this.databaseService = databaseService;
        this.syncExecutor = syncExecutor;
    }

    @Override
    public FactionId getFaction(UUID uuid) {
        return factionCache.getOrDefault(uuid, FactionId.NONE);
    }

    @Override
    public String getRank(UUID uuid) {
        return rankCache.getOrDefault(uuid, "Default");
    }

    @Override
    public void setFaction(UUID uuid, FactionId faction, String rank) {
        factionCache.put(uuid, faction);
        rankCache.put(uuid, rank == null ? "Default" : rank);
        saveFaction(uuid);
    }

    @Override
    public CompletableFuture<FactionId> loadFaction(UUID uuid) {
        return databaseService.queryAsync(connection -> {
            String sql = "SELECT faction_id FROM player_factions WHERE uuid = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return FactionId.valueOf(rs.getString("faction_id"));
                    }
                }
            } catch (SQLException | IllegalArgumentException e) {
                logger.log(Level.SEVERE, "Failed to load faction for " + uuid, e);
            }
            return FactionId.NONE;
        }).thenApply(faction -> {
            syncExecutor.accept(() -> factionCache.put(uuid, faction));
            return faction;
        });
    }

    @Override
    public CompletableFuture<String> loadRank(UUID uuid) {
        return databaseService.queryAsync(connection -> {
            String sql = "SELECT rank FROM player_factions WHERE uuid = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("rank");
                    }
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to load rank for " + uuid, e);
            }
            return "Default";
        }).thenApply(rank -> {
            syncExecutor.accept(() -> rankCache.put(uuid, rank));
            return rank;
        });
    }

    @Override
    public void unloadFaction(UUID uuid) {
        saveFaction(uuid);
        factionCache.remove(uuid);
        rankCache.remove(uuid);
    }

    private void saveFaction(UUID uuid) {
        FactionId faction = factionCache.get(uuid);
        String rank = rankCache.get(uuid);
        if (faction == null) return;

        databaseService.executeAsync(connection -> {
            String sql = "INSERT INTO player_factions (uuid, faction_id, rank) VALUES (?, ?, ?) " +
                         "ON CONFLICT(uuid) DO UPDATE SET faction_id = EXCLUDED.faction_id, rank = EXCLUDED.rank";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                pstmt.setString(2, faction.name());
                pstmt.setString(3, rank == null ? "Default" : rank);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to save faction for " + uuid, e);
            }
        });
    }

    @Override
    public void saveAll() {
        for (UUID uuid : factionCache.keySet()) {
            saveFaction(uuid);
        }
    }
}
