package ru.hrp.crime;

import ru.hrp.config.ConfigService;
import ru.hrp.core.DatabaseService;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CrimeManager implements CrimeService {
    private final Logger logger;
    private final DatabaseService databaseService;
    private final ConfigService configService;

    private final Map<CrimeId, CrimeDefinition> definitions = new HashMap<>();
    private final Map<UUID, List<CrimeRecord>> playerCrimes = new ConcurrentHashMap<>();

    public CrimeManager(Logger logger, DatabaseService databaseService, ConfigService configService) {
        this.logger = logger;
        this.databaseService = databaseService;
        this.configService = configService;
    }

    @Override
    public void loadDefinitions() {
        definitions.clear();
        var config = configService.getCrimes();
        for (String key : config.getKeys(false)) {
            try {
                CrimeId id = CrimeId.valueOf(key);
                String name = config.getString(key + ".name", key);
                long jailTime = config.getLong(key + ".jail_time", 0);
                definitions.put(id, new CrimeDefinition(id, name, jailTime));
            } catch (IllegalArgumentException e) {
                logger.warning("Invalid crime definition for: " + key);
            }
        }
        logger.info("Loaded " + definitions.size() + " crime definitions.");
    }

    @Override
    public CompletableFuture<List<CrimeRecord>> loadPlayerCrimes(UUID uuid) {
        return databaseService.queryAsync(connection -> {
            List<CrimeRecord> crimes = new ArrayList<>();
            String sql = "SELECT * FROM player_crimes WHERE uuid = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        crimes.add(new CrimeRecord(
                            rs.getInt("id"),
                            uuid,
                            CrimeId.valueOf(rs.getString("crime_id")),
                            rs.getLong("timestamp"),
                            rs.getInt("resolved") == 1
                        ));
                    }
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to load crimes for " + uuid, e);
            }
            return crimes;
        }).thenApply(crimes -> {
            playerCrimes.put(uuid, Collections.synchronizedList(crimes));
            return crimes;
        });
    }

    @Override
    public void unloadPlayerCrimes(UUID uuid) {
        playerCrimes.remove(uuid);
    }

    @Override
    public List<CrimeRecord> getActiveCrimes(UUID uuid) {
        return playerCrimes.getOrDefault(uuid, List.of()).stream()
            .filter(c -> !c.resolved())
            .toList();
    }

    @Override
    public List<CrimeRecord> getAllCrimes(UUID uuid) {
        return List.copyOf(playerCrimes.getOrDefault(uuid, List.of()));
    }

    @Override
    public CompletableFuture<CrimeRecord> reportCrime(UUID uuid, CrimeId crimeId) {
        long now = System.currentTimeMillis();
        return databaseService.queryAsync(connection -> {
            String sql = "INSERT INTO player_crimes (uuid, crime_id, timestamp, resolved) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, uuid.toString());
                pstmt.setString(2, crimeId.name());
                pstmt.setLong(3, now);
                pstmt.setInt(4, 0);
                pstmt.executeUpdate();

                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return new CrimeRecord(rs.getInt(1), uuid, crimeId, now, false);
                    }
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to report crime for " + uuid, e);
            }
            return null;
        }).thenApply(record -> {
            if (record != null) {
                playerCrimes.computeIfAbsent(uuid, k -> Collections.synchronizedList(new ArrayList<>())).add(record);
            }
            return record;
        });
    }

    @Override
    public CompletableFuture<Void> resolveCrime(int crimeId) {
        return databaseService.executeAsync(connection -> {
            String sql = "UPDATE player_crimes SET resolved = 1 WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setInt(1, crimeId);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to resolve crime " + crimeId, e);
            }
        }).thenRun(() -> {
            // Update in-memory if present
            playerCrimes.values().forEach(list -> {
                for (int i = 0; i < list.size(); i++) {
                    CrimeRecord r = list.get(i);
                    if (r.id() == crimeId) {
                        list.set(i, new CrimeRecord(r.id(), r.playerUuid(), r.crimeId(), r.timestamp(), true));
                        break;
                    }
                }
            });
        });
    }

    @Override
    public void saveAll() {
        // Crimes are saved immediately in this implementation (reportCrime/resolveCrime)
    }
}
