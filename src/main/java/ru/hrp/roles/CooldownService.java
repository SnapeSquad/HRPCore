package ru.hrp.roles;

import java.util.UUID;

public interface CooldownService {
    /**
     * Checks if an ability is on cooldown for a player.
     * @param uuid Player UUID.
     * @param abilityId Ability ID.
     * @return true if on cooldown.
     */
    boolean isOnCooldown(UUID uuid, String abilityId);

    /**
     * Gets the remaining cooldown time in milliseconds.
     * @param uuid Player UUID.
     * @param abilityId Ability ID.
     * @return Remaining milliseconds.
     */
    long getRemainingTime(UUID uuid, String abilityId);

    /**
     * Sets the cooldown for a player's ability.
     * @param uuid Player UUID.
     * @param abilityId Ability ID.
     * @param seconds Cooldown duration in seconds.
     */
    void setCooldown(UUID uuid, String abilityId, long seconds);

    /**
     * Removes the cooldown for a player's ability.
     */
    void removeCooldown(UUID uuid, String abilityId);
}
