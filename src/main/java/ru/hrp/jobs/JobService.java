package ru.hrp.jobs;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface JobService {
    void loadDefinitions();

    CompletableFuture<JobId> loadJob(UUID uuid);
    void unloadJob(UUID uuid);

    JobId getJob(UUID uuid);
    JobDefinition getDefinition(JobId jobId);

    void setJob(UUID uuid, JobId jobId);
    void processAction(UUID uuid, String actionId);

    void saveAll();
}
