package ru.hrp.core.commands;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.hrp.core.MessageService;
import ru.hrp.government.FactionId;
import ru.hrp.government.FactionService;

import java.util.UUID;

public class AdminFactionCommand implements SubCommand {
    private final FactionService factionService;
    private final MessageService messageService;

    public AdminFactionCommand(FactionService factionService, MessageService messageService) {
        this.factionService = factionService;
        this.messageService = messageService;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            messageService.sendMessage(sender, "admin.faction.usage");
            return;
        }

        String action = args[0].toLowerCase();
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            messageService.sendMessage(sender, "admin.player_not_found");
            return;
        }

        UUID uuid = target.getUniqueId();
        switch (action) {
            case "clear" -> {
                factionService.setFaction(uuid, FactionId.NONE, "Default");
                messageService.sendMessage(sender, "admin.faction.cleared", Placeholder.unparsed("player", target.getName()));
            }
            case "set" -> {
                if (args.length < 3) {
                    messageService.sendMessage(sender, "admin.faction.usage_set");
                    return;
                }
                try {
                    FactionId factionId = FactionId.valueOf(args[2].toUpperCase());
                    String rank = args.length > 3 ? args[3] : "Default";
                    factionService.setFaction(uuid, factionId, rank);
                    messageService.sendMessage(sender, "admin.faction.set",
                        TagResolver.resolver(
                            Placeholder.unparsed("player", target.getName()),
                            Placeholder.unparsed("faction", factionId.name()),
                            Placeholder.unparsed("rank", rank)
                        ));
                } catch (IllegalArgumentException e) {
                    messageService.sendMessage(sender, "admin.faction.invalid_faction");
                }
            }
            case "info" -> {
                FactionId faction = factionService.getFaction(uuid);
                String rank = factionService.getRank(uuid);
                messageService.sendMessage(sender, "admin.faction.info.header", Placeholder.unparsed("player", target.getName()));
                messageService.sendMessage(sender, "admin.faction.info.current",
                    TagResolver.resolver(
                        Placeholder.unparsed("faction", faction.name()),
                        Placeholder.unparsed("rank", rank)
                    ));
            }
            default -> messageService.sendMessage(sender, "admin.faction.unknown_action");
        }
    }

    @Override
    public String getPermission() {
        return "hrp.admin.faction";
    }
}
