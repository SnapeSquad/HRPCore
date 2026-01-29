package ru.hrp.core.commands;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.hrp.core.MessageService;
import ru.hrp.crime.CrimeId;
import ru.hrp.crime.CrimeService;
import ru.hrp.jail.JailService;

import java.util.UUID;

public class AdminJailCommand implements SubCommand {
    private final JailService jailService;
    private final CrimeService crimeService;
    private final MessageService messageService;
    private final String action;
    private final String permission;

    public AdminJailCommand(JailService jailService, CrimeService crimeService, MessageService messageService, String action, String permission) {
        this.jailService = jailService;
        this.crimeService = crimeService;
        this.messageService = messageService;
        this.action = action.toLowerCase();
        this.permission = permission;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (action.equals("jail")) {
            if (args.length < 2) {
                messageService.sendRawMessage(sender, "<red>Usage: /hrp admin jail <player> <time_seconds></red>");
                return;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                messageService.sendMessage(sender, "admin.player_not_found");
                return;
            }
            long time;
            try {
                time = Long.parseLong(args[1]);
            } catch (NumberFormatException e) {
                messageService.sendRawMessage(sender, "<red>Invalid time.</red>");
                return;
            }
            jailService.jailPlayer(target.getUniqueId(), time);
            messageService.sendMessage(sender, "admin.jailed",
                Placeholder.parsed("player", target.getName()),
                Placeholder.parsed("time", String.valueOf(time))
            );
        } else if (action.equals("release")) {
            if (args.length < 1) {
                messageService.sendRawMessage(sender, "<red>Usage: /hrp admin release <player></red>");
                return;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                messageService.sendMessage(sender, "admin.player_not_found");
                return;
            }
            jailService.releasePlayer(target.getUniqueId());
            messageService.sendMessage(sender, "admin.released",
                Placeholder.parsed("player", target.getName())
            );
        } else if (action.equals("crime")) {
            if (args.length < 3 || !args[0].equalsIgnoreCase("add")) {
                messageService.sendRawMessage(sender, "<red>Usage: /hrp admin crime add <player> <crime_id></red>");
                return;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                messageService.sendMessage(sender, "admin.player_not_found");
                return;
            }
            try {
                CrimeId crimeId = CrimeId.valueOf(args[2].toUpperCase());
                crimeService.reportCrime(target.getUniqueId(), crimeId);
                messageService.sendMessage(sender, "admin.crime_added",
                    Placeholder.parsed("player", target.getName()),
                    Placeholder.parsed("crime", crimeId.name())
                );
            } catch (IllegalArgumentException e) {
                messageService.sendRawMessage(sender, "<red>Invalid crime ID.</red>");
            }
        }
    }

    @Override
    public String getPermission() {
        return permission;
    }
}
