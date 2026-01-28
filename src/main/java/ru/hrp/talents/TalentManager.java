package ru.hrp.talents;

import ru.hrp.config.ConfigService;
import ru.hrp.core.DatabaseService;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TalentManager implements TalentService {
    private final Logger logger;
    private final DatabaseService databaseService;
    private final ConfigService configService;
    private final Consumer<Runnable> syncExecutor;

    private final Map<TalentId, TalentDefinition> definitions = new HashMap<>();
    private final Map<UUID, Map<TalentId, Integer>> playerTalents = new ConcurrentHashMap<>();

    public TalentManager(Logger logger, DatabaseService databaseService, ConfigService configService, Consumer<Runnable> syncExecutor) {
        this.logger = logger;
        this.databaseService = databaseService;
        this.configService = configService;
        this.syncExecutor = syncExecutor;
    }

    @Override
    public void loadDefinitions() {
        definitions.clear();
        var config = configService.getTalents();
        for (String key : config.getKeys(false)) {
            try {
                TalentId id = TalentId.valueOf(key);
                String name = config.getString(key + ".name", key);
                var description = config.getStringList(key + ".description");
                int maxLevel = config.getInt(key + ".max_level", 3);

                if (maxLevel < 1 || maxLevel > 3) {
                    throw new IllegalArgumentException("Max level must be between 1 and 3 for talent: " + key);
                }

                definitions.put(id, new TalentDefinition(id, name, description, maxLevel));
            } catch (IllegalArgumentException e) {
                logger.warning("Invalid talent definition for: " + key + ". " + e.getMessage());
            }
        }
        logger.info("Loaded " + definitions.size() + " talent definitions.");
    }

    @Override
    public CompletableFuture<Map<TalentId, Integer>> loadTalents(UUID uuid) {
        return databaseService.queryAsync(connection -> {
            Map<TalentId, Integer> talents = new HashMap<>();
            String sql = "SELECT talent_id, level FROM player_talents WHERE uuid = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        try {
                            TalentId id = TalentId.valueOf(rs.getString("talent_id"));
                            talents.put(id, rs.getInt("level"));
                        } catch (IllegalArgumentException e) {
                            // Ignore unknown talents
                        }
                    }
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to load talents for " + uuid, e);
            }
            return talents;
        }).thenCompose(talents -> {
            CompletableFuture<Map<TalentId, Integer>> future = new CompletableFuture<>();
            syncExecutor.accept(() -> {
                playerTalents.put(uuid, new ConcurrentHashMap<>(talents));
                future.complete(talents);
            });
            return future;
        });
    }

    @Override
    public void unloadTalents(UUID uuid) {
        Map<TalentId, Integer> talents = playerTalents.remove(uuid);
        if (talents != null) {
            savePlayerTalents(uuid, talents);
        }
    }

    private void savePlayerTalents(UUID uuid, Map<TalentId, Integer> talents) {
        databaseService.executeAsync(connection -> {
            String sql = "INSERT INTO player_talents (uuid, talent_id, level) VALUES (?, ?, ?) " +
                         "ON CONFLICT(uuid, talent_id) DO UPDATE SET level = EXCLUDED.level";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                for (var entry : talents.entrySet()) {
                    pstmt.setString(1, uuid.toString());
                    pstmt.setString(2, entry.getKey().name());
                    pstmt.setInt(3, entry.getValue());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to save talents for " + uuid, e);
            }
        });
    }

    @Override
    public Map<TalentId, Integer> getTalents(UUID uuid) {
        return playerTalents.getOrDefault(uuid, Map.of());
    }

    @Override
    public int getTalentLevel(UUID uuid, TalentId talentId) {
        return getTalents(uuid).getOrDefault(talentId, 0);
    }

    @Override
    public TalentDefinition getDefinition(TalentId talentId) {
        return definitions.get(talentId);
    }

    @Override
    public void setTalentLevel(UUID uuid, TalentId talentId, int level) {
        TalentDefinition definition = definitions.get(talentId);
        if (definition == null) return;

        if (level < 0) level = 0;
        if (level > definition.maxLevel()) level = definition.maxLevel();

        int finalLevel = level;
        playerTalents.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(talentId, finalLevel);
    }

    @Override
    public void saveAll() {
        playerTalents.forEach(this::savePlayerTalents);
    }
}
