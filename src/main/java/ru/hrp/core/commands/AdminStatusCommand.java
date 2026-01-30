package ru.hrp.core.commands;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.hrp.core.MessageService;
import ru.hrp.gui.GuiService;
import ru.hrp.gui.GuiType;

public class AdminStatusCommand implements SubCommand {
    private final GuiService guiService;
    private final MessageService messageService;

    public AdminStatusCommand(GuiService guiService, MessageService messageService) {
        this.guiService = guiService;
        this.messageService = messageService;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messageService.sendMessage(sender, "errors.only_players");
            return;
        }

        if (args.length < 1) {
            guiService.openGui(player, GuiType.STATUS);
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            messageService.sendMessage(sender, "admin.player_not_found");
            return;
        }

        // Open Status GUI for the admin, but viewing the target's data
        guiService.openGui(player, GuiType.STATUS, target);
        messageService.sendMessage(sender, "admin.status_opened", Placeholder.unparsed("player", target.getName()));
    }

    @Override
    public String getPermission() {
        return "hrp.admin.status";
    }
}
