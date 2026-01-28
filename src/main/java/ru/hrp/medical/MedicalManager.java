package ru.hrp.medical;

import ru.hrp.config.ConfigService;
import ru.hrp.core.DatabaseService;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MedicalManager implements MedicalService {
    private final Logger logger;
    private final DatabaseService databaseService;
    private final ConfigService configService;
    private final Consumer<Runnable> syncExecutor;

    private final Map<UUID, MedicalData> medicalCache = new ConcurrentHashMap<>();
    private final List<MedicalStateChangeListener> listeners = new ArrayList<>();

    public MedicalManager(Logger logger, DatabaseService databaseService, ConfigService configService, Consumer<Runnable> syncExecutor) {
        this.logger = logger;
        this.databaseService = databaseService;
        this.configService = configService;
        this.syncExecutor = syncExecutor;
    }

    @Override
    public CompletableFuture<MedicalData> loadMedicalData(UUID uuid) {
        return databaseService.queryAsync(connection -> {
            String sql = "SELECT * FROM player_medical WHERE uuid = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return new MedicalData(
                            uuid,
                            MedicalState.valueOf(rs.getString("state")),
                            rs.getLong("state_timestamp")
                        );
                    }
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to load medical data for " + uuid, e);
            }
            return new MedicalData(uuid, MedicalState.ALIVE, System.currentTimeMillis());
        }).thenCompose(data -> {
            CompletableFuture<MedicalData> future = new CompletableFuture<>();
            syncExecutor.accept(() -> {
                MedicalData finalData = resolveState(data);
                medicalCache.put(uuid, finalData);
                if (finalData.state() != data.state()) {
                    saveMedicalData(finalData);
                }
                future.complete(finalData);
            });
            return future;
        });
    }

    private MedicalData resolveState(MedicalData data) {
        if (data.state() == MedicalState.CRITICAL) {
            long timeout = configService.getConfig().getLong("medical.critical_timeout", 300) * 1000;
            if (System.currentTimeMillis() >= data.stateTimestamp() + timeout) {
                return new MedicalData(data.uuid(), MedicalState.DEAD, data.stateTimestamp() + timeout);
            }
        }
        return data;
    }

    @Override
    public void unloadMedicalData(UUID uuid) {
        MedicalData data = medicalCache.remove(uuid);
        if (data != null) {
            saveMedicalData(data);
        }
    }

    private void saveMedicalData(MedicalData data) {
        databaseService.executeAsync(connection -> {
            String sql = "INSERT INTO player_medical (uuid, state, state_timestamp) VALUES (?, ?, ?) " +
                         "ON CONFLICT(uuid) DO UPDATE SET state = EXCLUDED.state, state_timestamp = EXCLUDED.state_timestamp";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, data.uuid().toString());
                pstmt.setString(2, data.state().name());
                pstmt.setLong(3, data.stateTimestamp());
                pstmt.executeUpdate();
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to save medical data for " + data.uuid(), e);
            }
        });
    }

    @Override
    public MedicalState getMedicalState(UUID uuid) {
        MedicalData data = medicalCache.get(uuid);
        if (data == null) return MedicalState.ALIVE;

        MedicalData resolved = resolveState(data);
        if (resolved != data) {
            medicalCache.put(uuid, resolved);
            saveMedicalData(resolved);
            if (resolved.state() == MedicalState.DEAD) {
                triggerDeathFinal(uuid);
            }
        }
        return resolved.state();
    }

    @Override
    public MedicalData getMedicalData(UUID uuid) {
        return medicalCache.get(uuid);
    }

    @Override
    public CompletableFuture<Void> setMedicalState(UUID uuid, MedicalState state) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        syncExecutor.accept(() -> {
            MedicalData data = new MedicalData(uuid, state, System.currentTimeMillis());
            medicalCache.put(uuid, data);
            saveMedicalData(data);

            switch (state) {
                case CRITICAL -> triggerEnterCritical(uuid);
                case ALIVE -> triggerRevive(uuid);
                case DEAD -> triggerDeathFinal(uuid);
            }
            future.complete(null);
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> revive(UUID uuid) {
        return setMedicalState(uuid, MedicalState.ALIVE);
    }

    @Override
    public CompletableFuture<Void> kill(UUID uuid) {
        return setMedicalState(uuid, MedicalState.DEAD);
    }

    @Override
    public void registerChangeListener(MedicalStateChangeListener listener) {
        listeners.add(listener);
    }

    private void triggerEnterCritical(UUID uuid) {
        listeners.forEach(l -> l.onEnterCritical(uuid));
    }

    private void triggerRevive(UUID uuid) {
        listeners.forEach(l -> l.onRevive(uuid));
    }

    private void triggerDeathFinal(UUID uuid) {
        listeners.forEach(l -> l.onDeathFinal(uuid));
    }

    @Override
    public void saveAll() {
        medicalCache.forEach((uuid, data) -> saveMedicalData(data));
    }
}
