package ru.hrp.core.commands;

import org.bukkit.command.CommandSender;

import java.util.List;

public interface SubCommand {
    void execute(CommandSender sender, String[] args);
    String getPermission();
}
