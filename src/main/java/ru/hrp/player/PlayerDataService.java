package ru.hrp.player;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface PlayerDataService {
    /**
     * Loads player data from the database into memory.
     * If the player does not exist, a new record is created.
     * This operation is async but ensures cache update happens on the main thread.
     */
    CompletableFuture<RPPlayer> loadPlayerData(UUID uuid, String name);

    /**
     * Saves the player data to the database and removes it from memory.
     */
    void unloadPlayerData(UUID uuid);

    /**
     * Saves the current state of a player to the database.
     */
    CompletableFuture<Void> savePlayerData(RPPlayer player);

    /**
     * Retrieves a player from the in-memory cache.
     */
    Optional<RPPlayer> getPlayer(UUID uuid);
}
