package ru.hrp.roles;

import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface RoleService {
    /**
     * Loads role definitions from configuration.
     */
    void loadDefinitions();

    /**
     * Loads a player's active role from the database.
     */
    CompletableFuture<RoleId> loadRole(UUID uuid);

    /**
     * Unloads a player's role from memory.
     */
    void unloadRole(UUID uuid);

    /**
     * Gets the active role ID for a player.
     */
    RoleId getActiveRoleId(UUID uuid);

    /**
     * Assigns a role to a player and returns the role card item.
     * This operation updates memory and database.
     */
    CompletableFuture<ItemStack> assignRole(UUID uuid, RoleId roleId);

    /**
     * Removes the active role from a player.
     */
    CompletableFuture<Void> removeRole(UUID uuid);

    /**
     * Saves all cached roles to the database.
     */
    void saveAll();
}
