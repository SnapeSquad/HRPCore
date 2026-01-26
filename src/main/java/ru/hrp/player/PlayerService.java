package ru.hrp.player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface PlayerService {
    CompletableFuture<Void> savePlayer(HRPPlayer player);
    CompletableFuture<HRPPlayer> loadPlayer(UUID uuid);
    void handlePlayerJoin(UUID uuid, String name);
    void handlePlayerQuit(UUID uuid);
}
