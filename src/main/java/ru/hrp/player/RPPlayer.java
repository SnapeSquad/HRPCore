package ru.hrp.player;

import ru.hrp.roles.RoleId;
import ru.hrp.talents.TalentId;

import java.util.Collections;
import java.util.Map;
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
    RoleId role,
    Map<TalentId, Integer> talents
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
            RoleId.NONE,
            Collections.emptyMap()
        );
    }
}
