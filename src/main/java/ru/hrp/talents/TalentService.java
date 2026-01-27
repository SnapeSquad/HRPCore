package ru.hrp.talents;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface TalentService {
    /**
     * Loads talent definitions from configuration.
     */
    void loadDefinitions();

    /**
     * Loads a player's talents from the database.
     */
    CompletableFuture<Map<TalentId, Integer>> loadTalents(UUID uuid);

    /**
     * Unloads a player's talents from memory.
     */
    void unloadTalents(UUID uuid);

    /**
     * Gets the current talent levels for a player.
     */
    Map<TalentId, Integer> getTalents(UUID uuid);

    /**
     * Gets the level of a specific talent for a player.
     */
    int getTalentLevel(UUID uuid, TalentId talentId);

    /**
     * Sets a player's talent level.
     */
    void setTalentLevel(UUID uuid, TalentId talentId, int level);

    /**
     * Saves all cached talents to the database.
     */
    void saveAll();
}
