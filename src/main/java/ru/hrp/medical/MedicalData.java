package ru.hrp.medical;

import java.util.UUID;

public record MedicalData(
    UUID uuid,
    MedicalState state,
    long stateTimestamp
) {}
