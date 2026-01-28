package ru.hrp.jail;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface JailService {
    CompletableFuture<JailState> loadJailState(UUID uuid);
    void unloadJailState(UUID uuid);

    boolean isJailed(UUID uuid);
    JailState getJailState(UUID uuid);

    CompletableFuture<Void> jailPlayer(UUID uuid, long seconds);
    CompletableFuture<Void> releasePlayer(UUID uuid);

    void saveAll();
}
