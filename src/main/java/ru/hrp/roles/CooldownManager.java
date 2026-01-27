package ru.hrp.roles;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownManager implements CooldownService {
    // Key: UUID + ":" + AbilityId, Value: End timestamp (millis)
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();

    @Override
    public boolean isOnCooldown(UUID uuid, String abilityId) {
        return getRemainingTime(uuid, abilityId) > 0;
    }

    @Override
    public long getRemainingTime(UUID uuid, String abilityId) {
        String key = uuid.toString() + ":" + abilityId;
        Long endTime = cooldowns.get(key);
        if (endTime == null) return 0;

        long remaining = endTime - System.currentTimeMillis();
        if (remaining <= 0) {
            cooldowns.remove(key);
            return 0;
        }
        return remaining;
    }

    @Override
    public void setCooldown(UUID uuid, String abilityId, long seconds) {
        if (seconds <= 0) return;
        String key = uuid.toString() + ":" + abilityId;
        cooldowns.put(key, System.currentTimeMillis() + (seconds * 1000));
    }

    @Override
    public void removeCooldown(UUID uuid, String abilityId) {
        String key = uuid.toString() + ":" + abilityId;
        cooldowns.remove(key);
    }
}
