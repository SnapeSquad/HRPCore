package ru.hrp.roles;

public record AbilityDefinition(
    String id,
    String name,
    String description,
    AbilityType type,
    AbilityTrigger trigger,
    long cooldownSeconds
) {}
