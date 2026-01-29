package ru.hrp.core.commands;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.hrp.core.MessageService;
import ru.hrp.talents.TalentId;
import ru.hrp.talents.TalentService;

import java.util.UUID;

public class AdminTalentCommand implements SubCommand {
    private final TalentService talentService;
    private final MessageService messageService;

    public AdminTalentCommand(TalentService talentService, MessageService messageService) {
        this.talentService = talentService;
        this.messageService = messageService;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 4) {
            messageService.sendRawMessage(sender, "<red>Usage: /hrp admin talent set <player> <talent> <level></red>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            messageService.sendMessage(sender, "admin.player_not_found");
            return;
        }

        TalentId talentId;
        try {
            talentId = TalentId.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException e) {
            messageService.sendRawMessage(sender, "<red>Invalid talent ID.</red>");
            return;
        }

        int level;
        try {
            level = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            messageService.sendRawMessage(sender, "<red>Invalid level.</red>");
            return;
        }

        UUID uuid = target.getUniqueId();
        talentService.setTalentLevel(uuid, talentId, level);

        messageService.sendMessage(sender, "admin.talent_updated",
            Placeholder.parsed("player", target.getName()),
            Placeholder.parsed("talent", talentId.name()),
            Placeholder.parsed("level", String.valueOf(talentService.getTalentLevel(uuid, talentId)))
        );
    }

    @Override
    public String getPermission() {
        return "hrp.admin.talent";
    }
}
