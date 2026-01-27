package ru.hrp.crime;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface CrimeService {
    void loadDefinitions();

    CompletableFuture<List<CrimeRecord>> loadPlayerCrimes(UUID uuid);
    void unloadPlayerCrimes(UUID uuid);

    List<CrimeRecord> getActiveCrimes(UUID uuid);
    List<CrimeRecord> getAllCrimes(UUID uuid);

    CompletableFuture<CrimeRecord> reportCrime(UUID uuid, CrimeId crimeId);
    CompletableFuture<Void> resolveCrime(int crimeId);

    void saveAll();
}
