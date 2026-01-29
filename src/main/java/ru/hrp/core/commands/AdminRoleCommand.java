package ru.hrp.core.commands;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.hrp.core.MessageService;
import ru.hrp.roles.RoleId;
import ru.hrp.roles.RoleService;

import java.util.UUID;

public class AdminRoleCommand implements SubCommand {
    private final RoleService roleService;
    private final MessageService messageService;

    public AdminRoleCommand(RoleService roleService, MessageService messageService) {
        this.roleService = roleService;
        this.messageService = messageService;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            messageService.sendRawMessage(sender, "<red>Usage: /hrp admin role <set|clear> <player> [role]</red>");
            return;
        }

        String action = args[0].toLowerCase();
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            messageService.sendMessage(sender, "admin.player_not_found");
            return;
        }

        UUID uuid = target.getUniqueId();
        if (action.equals("clear")) {
            roleService.removeRole(uuid);
            messageService.sendMessage(sender, "admin.role_updated",
                Placeholder.parsed("player", target.getName()),
                Placeholder.parsed("role", "NONE")
            );
            return;
        }

        if (action.equals("set")) {
            if (args.length < 3) {
                messageService.sendRawMessage(sender, "<red>Usage: /hrp admin role set <player> <role></red>");
                return;
            }
            try {
                RoleId roleId = RoleId.valueOf(args[2].toUpperCase());
                roleService.assignRole(uuid, roleId).thenAccept(item -> {
                    if (item != null) {
                        target.getInventory().addItem(item);
                    }
                });
                messageService.sendMessage(sender, "admin.role_updated",
                    Placeholder.parsed("player", target.getName()),
                    Placeholder.parsed("role", roleId.name())
                );
            } catch (IllegalArgumentException e) {
                messageService.sendRawMessage(sender, "<red>Invalid role ID.</red>");
            }
        }
    }

    @Override
    public String getPermission() {
        return "hrp.admin.role";
    }
}
