package ru.hrp.crime;

import java.util.UUID;

public record JailState(
    UUID playerUuid,
    long releaseTimestamp
) {
    public boolean isExpired() {
        return System.currentTimeMillis() >= releaseTimestamp;
    }
}
