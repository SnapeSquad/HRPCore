package ru.hrp.government;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface FactionService {
    FactionId getFaction(UUID uuid);
    String getRank(UUID uuid);
    void setFaction(UUID uuid, FactionId faction, String rank);

    CompletableFuture<FactionId> loadFaction(UUID uuid);
    CompletableFuture<String> loadRank(UUID uuid);
    void unloadFaction(UUID uuid);

    void saveAll();
}
