package ru.hrp.roles;

import ru.hrp.government.FactionId;
import ru.hrp.government.FactionService;
import ru.hrp.medical.MedicalService;
import ru.hrp.medical.MedicalState;

import java.util.UUID;

public class MedicReviveExecutor implements AbilityExecutor {
    private final MedicalService medicalService;
    private final FactionService factionService;

    public MedicReviveExecutor(MedicalService medicalService, FactionService factionService) {
        this.medicalService = medicalService;
        this.factionService = factionService;
    }

    @Override
    public boolean execute(UUID uuid, AbilityContext context) {
        if (factionService.getFaction(uuid) != FactionId.MEDICAL) return false;

        UUID targetUuid = (UUID) context.data().get("target_uuid");
        if (targetUuid == null) return false;

        if (medicalService.getMedicalState(targetUuid) == MedicalState.CRITICAL) {
            medicalService.revive(targetUuid);
            return true;
        }

        return false;
    }
}
