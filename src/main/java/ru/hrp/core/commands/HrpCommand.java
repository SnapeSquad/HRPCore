package ru.hrp.core.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import ru.hrp.core.MessageService;

import java.util.HashMap;
import java.util.Map;

public class HrpCommand implements CommandExecutor {
    private final MessageService messageService;
    private final Map<String, SubCommand> routes = new HashMap<>();

    public HrpCommand(MessageService messageService) {
        this.messageService = messageService;
    }

    public void registerRoute(String name, SubCommand subCommand) {
        routes.put(name.toLowerCase(), subCommand);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            messageService.sendRawMessage(sender, "<gold>HRPCore version 1.0.0</gold>");
            return true;
        }

        String routeName = args[0].toLowerCase();
        SubCommand route = routes.get(routeName);

        if (route == null) {
            messageService.sendRawMessage(sender, "<red>Unknown command. Try /hrp admin</red>");
            return true;
        }

        if (!sender.hasPermission(route.getPermission()) && !sender.hasPermission("hrp.admin")) {
            messageService.sendMessage(sender, "errors.no_permission");
            return true;
        }

        String[] routeArgs = new String[args.length - 1];
        System.arraycopy(args, 1, routeArgs, 0, args.length - 1);
        route.execute(sender, routeArgs);

        return true;
    }
}
