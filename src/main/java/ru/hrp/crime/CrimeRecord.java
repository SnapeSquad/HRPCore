package ru.hrp.crime;

import java.util.UUID;

public record CrimeRecord(
    int id,
    UUID playerUuid,
    CrimeId crimeId,
    long timestamp,
    boolean resolved
) {}
