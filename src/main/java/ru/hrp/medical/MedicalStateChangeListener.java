package ru.hrp.medical;

import java.util.UUID;

public interface MedicalStateChangeListener {
    void onEnterCritical(UUID uuid);
    void onRevive(UUID uuid);
    void onDeathFinal(UUID uuid);
}
