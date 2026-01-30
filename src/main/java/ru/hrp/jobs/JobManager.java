package ru.hrp.jobs;

import ru.hrp.config.ConfigService;
import ru.hrp.core.DatabaseService;
import ru.hrp.economy.EconomyService;
import ru.hrp.talents.TalentId;
import ru.hrp.talents.TalentService;

import java.math.BigDecimal;
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

public class JobManager implements JobService {
    private final Logger logger;
    private final DatabaseService databaseService;
    private final EconomyService economyService;
    private final TalentService talentService;
    private final ConfigService configService;
    private final Consumer<Runnable> syncExecutor;

    private final Map<JobId, JobDefinition> definitions = new HashMap<>();
    private final Map<UUID, JobId> activeJobs = new ConcurrentHashMap<>();

    public JobManager(Logger logger, DatabaseService databaseService, EconomyService economyService,
                      TalentService talentService, ConfigService configService, Consumer<Runnable> syncExecutor) {
        this.logger = logger;
        this.databaseService = databaseService;
        this.economyService = economyService;
        this.talentService = talentService;
        this.configService = configService;
        this.syncExecutor = syncExecutor;
    }

    @Override
    public void loadDefinitions() {
        definitions.clear();
        var config = configService.getJobs();
        for (String key : config.getKeys(false)) {
            try {
                JobId id = JobId.valueOf(key);
                String name = config.getString(key + ".name", key);
                Map<String, BigDecimal> rewards = new HashMap<>();

                var rewardSection = config.getConfigurationSection(key + ".rewards");
                if (rewardSection != null) {
                    for (String action : rewardSection.getKeys(false)) {
                        rewards.put(action, new BigDecimal(rewardSection.getString(action, "0")));
                    }
                }

                definitions.put(id, new JobDefinition(id, name, rewards));
            } catch (IllegalArgumentException e) {
                logger.warning("Invalid job definition for: " + key);
            }
        }
        logger.info("Loaded " + definitions.size() + " job definitions.");
    }

    @Override
    public CompletableFuture<JobId> loadJob(UUID uuid) {
        return databaseService.queryAsync(connection -> {
            String sql = "SELECT job_id FROM player_jobs WHERE uuid = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        try {
                            return JobId.valueOf(rs.getString("job_id"));
                        } catch (IllegalArgumentException e) {
                            return JobId.NONE;
                        }
                    }
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to load job for " + uuid, e);
            }
            return JobId.NONE;
        }).thenCompose(jobId -> {
            CompletableFuture<JobId> future = new CompletableFuture<>();
            syncExecutor.accept(() -> {
                activeJobs.put(uuid, jobId);
                future.complete(jobId);
            });
            return future;
        });
    }

    @Override
    public void unloadJob(UUID uuid) {
        JobId jobId = activeJobs.remove(uuid);
        if (jobId != null && jobId != JobId.NONE) {
            saveJob(uuid, jobId);
        }
    }

    private void saveJob(UUID uuid, JobId jobId) {
        databaseService.executeAsync(connection -> {
            String sql = "INSERT INTO player_jobs (uuid, job_id, level) VALUES (?, ?, ?) " +
                         "ON CONFLICT(uuid) DO UPDATE SET job_id = EXCLUDED.job_id";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                pstmt.setString(2, jobId.name());
                pstmt.setInt(3, 1); // Level placeholder
                pstmt.executeUpdate();
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to save job for " + uuid, e);
            }
        });
    }

    @Override
    public JobId getJob(UUID uuid) {
        return activeJobs.getOrDefault(uuid, JobId.NONE);
    }

    @Override
    public JobDefinition getDefinition(JobId jobId) {
        return definitions.get(jobId);
    }

    @Override
    public void setJob(UUID uuid, JobId jobId) {
        syncExecutor.accept(() -> {
            activeJobs.put(uuid, jobId);
            saveJob(uuid, jobId);
        });
    }

    @Override
    public void processAction(UUID uuid, String actionId) {
        JobId jobId = activeJobs.get(uuid);
        if (jobId == null || jobId == JobId.NONE) return;

        JobDefinition def = definitions.get(jobId);
        if (def == null) return;

        BigDecimal baseReward = def.rewards().get(actionId);
        if (baseReward == null) return;

        int strength = talentService.getTalentLevel(uuid, TalentId.STRENGTH);
        // Scaling: +20% per level of strength
        BigDecimal multiplier = BigDecimal.valueOf(1.0 + (strength * 0.2));
        BigDecimal finalReward = baseReward.multiply(multiplier);

        economyService.deposit(uuid, finalReward);
    }

    @Override
    public void saveAll() {
        activeJobs.forEach((uuid, jobId) -> {
            if (jobId != JobId.NONE) {
                saveJob(uuid, jobId);
            }
        });
    }
}
