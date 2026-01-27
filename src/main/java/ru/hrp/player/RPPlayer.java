package ru.hrp.player;

import java.util.Set;
import java.util.UUID;

/**
 * Immutable domain model representing a player in the RP system.
 */
public record RPPlayer(
    UUID uuid,
    String lastKnownName,
    long firstJoin,
    long lastSeen,

    // Placeholders for future systems
    String role,
    String job,
    Set<String> talents
) {
    /**
     * Creates a new RPPlayer with default values for new players.
     */
    public static RPPlayer createDefault(UUID uuid, String name) {
        long now = System.currentTimeMillis();
        return new RPPlayer(
            uuid,
            name,
            now,
            now,
            "NONE",
            "NONE",
            Set.of()
        );
    }
}
