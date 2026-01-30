package ru.hrp.roles;

import ru.hrp.crime.CrimeRecord;
import ru.hrp.crime.CrimeService;
import ru.hrp.government.FactionId;
import ru.hrp.government.FactionService;
import ru.hrp.jail.JailService;

import java.util.List;
import java.util.UUID;

public class PoliceArrestExecutor implements AbilityExecutor {
    private final CrimeService crimeService;
    private final JailService jailService;
    private final FactionService factionService;

    public PoliceArrestExecutor(CrimeService crimeService, JailService jailService, FactionService factionService) {
        this.crimeService = crimeService;
        this.jailService = jailService;
        this.factionService = factionService;
    }

    @Override
    public boolean execute(UUID uuid, AbilityContext context) {
        if (factionService.getFaction(uuid) != FactionId.POLICE) return false;

        UUID targetUuid = (UUID) context.data().get("target_uuid");
        if (targetUuid == null) return false;

        List<CrimeRecord> activeCrimes = crimeService.getActiveCrimes(targetUuid);
        if (activeCrimes.isEmpty()) return false;

        // Sum up jail time for all active crimes
        // Note: Step 8 didn't explicitly link crime definitions to records,
        // but we can assume default jail time from CrimeService or just use a fixed amount per crime for Step 12.
        // Actually, let's just use 300s per crime for simplicity in Step 12 Content.
        long totalJailTime = activeCrimes.size() * 300L;

        jailService.jailPlayer(targetUuid, totalJailTime);

        // Resolve all crimes
        for (CrimeRecord crime : activeCrimes) {
            crimeService.resolveCrime(crime.id());
        }

        return true;
    }
}
