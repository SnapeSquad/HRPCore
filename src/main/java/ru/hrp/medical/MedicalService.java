package ru.hrp.medical;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface MedicalService {
    CompletableFuture<MedicalData> loadMedicalData(UUID uuid);
    void unloadMedicalData(UUID uuid);

    MedicalState getMedicalState(UUID uuid);
    MedicalData getMedicalData(UUID uuid);

    CompletableFuture<Void> setMedicalState(UUID uuid, MedicalState state);
    CompletableFuture<Void> revive(UUID uuid);
    CompletableFuture<Void> kill(UUID uuid);

    void registerChangeListener(MedicalStateChangeListener listener);

    void saveAll();
}
