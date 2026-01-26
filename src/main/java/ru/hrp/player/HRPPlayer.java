package ru.hrp.player;

import java.util.UUID;

public record HRPPlayer(
    UUID uuid,
    String lastName,
    long firstJoin,
    long lastSeen
) {}
