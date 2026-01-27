package ru.hrp.roles;

import java.util.List;

public record RoleDefinition(
    RoleId id,
    String name,
    List<String> description
) {}
