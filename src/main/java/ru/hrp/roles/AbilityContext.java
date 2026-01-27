package ru.hrp.roles;

import java.util.Map;

/**
 * Generic context for ability execution.
 * Avoids direct Bukkit dependencies in the engine.
 */
public record AbilityContext(
    Map<String, Object> data
) {}
