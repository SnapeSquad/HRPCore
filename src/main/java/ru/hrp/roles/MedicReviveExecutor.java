package ru.hrp.roles;

import ru.hrp.medical.MedicalService;
import ru.hrp.medical.MedicalState;

import java.util.UUID;

public class MedicReviveExecutor implements AbilityExecutor {
    private final MedicalService medicalService;

    public MedicReviveExecutor(MedicalService medicalService) {
        this.medicalService = medicalService;
    }

    @Override
    public boolean execute(UUID uuid, AbilityContext context) {
        UUID targetUuid = (UUID) context.data().get("target_uuid");
        if (targetUuid == null) return false;

        if (medicalService.getMedicalState(targetUuid) == MedicalState.CRITICAL) {
            medicalService.revive(targetUuid);
            return true;
        }

        return false;
    }
}
