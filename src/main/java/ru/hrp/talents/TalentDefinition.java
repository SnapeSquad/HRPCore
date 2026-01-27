package ru.hrp.talents;

import java.util.List;

public record TalentDefinition(
    TalentId id,
    String name,
    List<String> description,
    int maxLevel
) {}
