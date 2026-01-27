package ru.hrp.roles;

import java.util.UUID;

public interface AbilityService {
    /**
     * Loads ability definitions from configuration.
     */
    void loadDefinitions();

    /**
     * Registers an executor for a specific ability ID.
     */
    void registerExecutor(String abilityId, AbilityExecutor executor);

    /**
     * Attempts to activate an ability for a player.
     * @param uuid Player UUID.
     * @param abilityId Ability ID.
     * @param context Execution context.
     * @return true if activated successfully.
     */
    boolean activate(UUID uuid, String abilityId, AbilityContext context);

    /**
     * Gets the definition for an ability ID.
     */
    AbilityDefinition getDefinition(String abilityId);
}
