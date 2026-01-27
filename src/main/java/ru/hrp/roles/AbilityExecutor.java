package ru.hrp.roles;

import java.util.UUID;

public interface AbilityExecutor {
    /**
     * Executes the ability for a specific player.
     * @param uuid The player's UUID.
     * @param context The execution context.
     * @return true if execution was successful and cooldown should be applied.
     */
    boolean execute(UUID uuid, AbilityContext context);
}
