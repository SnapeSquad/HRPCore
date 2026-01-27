package ru.hrp.crime;

public record CrimeDefinition(
    CrimeId id,
    String name,
    long defaultJailTimeSeconds
) {}
