package ru.hrp.bank;

import java.util.UUID;

public interface CreditSanctionHook {
    /**
     * Called when a player's credit is marked as DEFAULTED.
     * @param uuid Player UUID.
     * @param credit The defaulted credit record.
     */
    void onCreditDefault(UUID uuid, CreditRecord credit);
}
