package ru.hrp.core.commands;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.hrp.core.MessageService;
import ru.hrp.medical.MedicalService;
import ru.hrp.medical.MedicalState;

import java.util.UUID;

public class AdminMedicalCommand implements SubCommand {
    private final MedicalService medicalService;
    private final MessageService messageService;
    private final String action;

    public AdminMedicalCommand(MedicalService medicalService, MessageService messageService, String action) {
        this.medicalService = medicalService;
        this.messageService = messageService;
        this.action = action.toLowerCase();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            messageService.sendRawMessage(sender, "<red>Usage: /hrp admin " + action + " <player></red>");
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            messageService.sendMessage(sender, "admin.player_not_found");
            return;
        }

        UUID uuid = target.getUniqueId();
        if (action.equals("revive") || action.equals("medical")) {
            // Note: 'medical' default to revive for now or just generic
            medicalService.revive(uuid);
            messageService.sendRawMessage(sender, "<green>Revived " + target.getName() + ".</green>");
        } else if (action.equals("kill")) {
            medicalService.kill(uuid);
            messageService.sendRawMessage(sender, "<red>Killed " + target.getName() + " (Medical state: DEAD).</red>");
        }
    }

    @Override
    public String getPermission() {
        return "hrp.admin.medical";
    }
}
