package ru.hrp.jail;

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

public class JailManager implements JailService {
    private final Logger logger;
    private final DatabaseService databaseService;
    private final Consumer<Runnable> syncExecutor;
    private final Map<UUID, JailState> jailCache = new ConcurrentHashMap<>();

    public JailManager(Logger logger, DatabaseService databaseService, Consumer<Runnable> syncExecutor) {
        this.logger = logger;
        this.databaseService = databaseService;
        this.syncExecutor = syncExecutor;
    }

    @Override
    public CompletableFuture<JailState> loadJailState(UUID uuid) {
        return databaseService.queryAsync(connection -> {
            String sql = "SELECT release_timestamp FROM player_jail WHERE uuid = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return new JailState(uuid, rs.getLong("release_timestamp"));
                    }
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to load jail state for " + uuid, e);
            }
            return null;
        }).thenCompose(state -> {
            CompletableFuture<JailState> future = new CompletableFuture<>();
            syncExecutor.accept(() -> {
                if (state != null) {
                    jailCache.put(uuid, state);
                }
                future.complete(state);
            });
            return future;
        });
    }

    @Override
    public void unloadJailState(UUID uuid) {
        JailState state = jailCache.remove(uuid);
        if (state != null) {
            saveJailState(state);
        }
    }

    private void saveJailState(JailState state) {
        databaseService.executeAsync(connection -> {
            String sql = "INSERT INTO player_jail (uuid, release_timestamp) VALUES (?, ?) " +
                         "ON CONFLICT(uuid) DO UPDATE SET release_timestamp = EXCLUDED.release_timestamp";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, state.playerUuid().toString());
                pstmt.setLong(2, state.releaseTimestamp());
                pstmt.executeUpdate();
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to save jail state for " + state.playerUuid(), e);
            }
        });
    }

    @Override
    public boolean isJailed(UUID uuid) {
        JailState state = jailCache.get(uuid);
        return state != null && !state.isExpired();
    }

    @Override
    public JailState getJailState(UUID uuid) {
        return jailCache.get(uuid);
    }

    @Override
    public CompletableFuture<Void> jailPlayer(UUID uuid, long seconds) {
        long releaseTimestamp = System.currentTimeMillis() + (seconds * 1000);
        JailState state = new JailState(uuid, releaseTimestamp);
        jailCache.put(uuid, state);
        return databaseService.executeAsync(connection -> {
            String sql = "INSERT INTO player_jail (uuid, release_timestamp) VALUES (?, ?) " +
                         "ON CONFLICT(uuid) DO UPDATE SET release_timestamp = EXCLUDED.release_timestamp";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                pstmt.setLong(2, releaseTimestamp);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to jail player " + uuid, e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> releasePlayer(UUID uuid) {
        jailCache.remove(uuid);
        return databaseService.executeAsync(connection -> {
            String sql = "DELETE FROM player_jail WHERE uuid = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                pstmt.executeUpdate();
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to release player " + uuid, e);
            }
        });
    }

    @Override
    public void saveAll() {
        jailCache.forEach((uuid, state) -> saveJailState(state));
    }
}
