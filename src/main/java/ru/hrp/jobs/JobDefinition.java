package ru.hrp.jobs;

import java.math.BigDecimal;
import java.util.Map;

public record JobDefinition(
    JobId id,
    String name,
    Map<String, BigDecimal> rewards
) {}
