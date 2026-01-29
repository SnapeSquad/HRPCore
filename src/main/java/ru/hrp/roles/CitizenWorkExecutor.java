package ru.hrp.roles;

import ru.hrp.economy.EconomyService;
import ru.hrp.talents.TalentId;
import ru.hrp.talents.TalentService;

import java.math.BigDecimal;
import java.util.UUID;

public class CitizenWorkExecutor implements AbilityExecutor {
    private final EconomyService economyService;
    private final TalentService talentService;

    public CitizenWorkExecutor(EconomyService economyService, TalentService talentService) {
        this.economyService = economyService;
        this.talentService = talentService;
    }

    @Override
    public boolean execute(UUID uuid, AbilityContext context) {
        int strengthLevel = talentService.getTalentLevel(uuid, TalentId.STRENGTH);

        // Base pay $50, increases by $25 per strength level
        BigDecimal pay = BigDecimal.valueOf(50 + (strengthLevel * 25));

        economyService.deposit(uuid, pay);
        return true;
    }
}
