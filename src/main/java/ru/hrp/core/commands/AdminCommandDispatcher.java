package ru.hrp.core.commands;

import org.bukkit.command.CommandSender;
import ru.hrp.core.MessageService;

import java.util.HashMap;
import java.util.Map;

public class AdminCommandDispatcher implements SubCommand {
    private final MessageService messageService;
    private final Map<String, SubCommand> subCommands = new HashMap<>();

    public AdminCommandDispatcher(MessageService messageService) {
        this.messageService = messageService;
    }

    public void registerSubCommand(String name, SubCommand subCommand) {
        subCommands.put(name.toLowerCase(), subCommand);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            messageService.sendRawMessage(sender, "<red>Usage: /hrp admin <subcommand></red>");
            return;
        }

        String sub = args[0].toLowerCase();
        SubCommand subCommand = subCommands.get(sub);

        if (subCommand == null) {
            messageService.sendRawMessage(sender, "<red>Unknown admin subcommand: " + sub + "</red>");
            return;
        }

        if (!sender.hasPermission(subCommand.getPermission()) && !sender.hasPermission("hrp.admin")) {
            messageService.sendMessage(sender, "errors.no_permission");
            return;
        }

        String[] subArgs = new String[args.length - 1];
        System.arraycopy(args, 1, subArgs, 0, args.length - 1);
        subCommand.execute(sender, subArgs);
    }

    @Override
    public String getPermission() {
        return "hrp.admin";
    }
}
