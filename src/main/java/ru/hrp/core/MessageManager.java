package ru.hrp.core;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import ru.hrp.config.ConfigService;

import java.util.Objects;

public class MessageManager implements MessageService {
    private final ConfigService configService;
    private final MiniMessage miniMessage;

    public MessageManager(ConfigService configService) {
        this.configService = configService;
        this.miniMessage = MiniMessage.miniMessage();
    }

    @Override
    public Component parse(String key, TagResolver... resolvers) {
        String message = configService.getMessages().getString(key);
        if (message == null) {
            return miniMessage.deserialize("<red>Missing message: " + key + "</red>");
        }

        String prefix = configService.getMessages().getString("prefix", "");
        if (!key.equals("prefix") && !prefix.isEmpty()) {
            message = prefix + message;
        }

        return miniMessage.deserialize(message, resolvers);
    }

    @Override
    public void sendMessage(CommandSender sender, String key, TagResolver... resolvers) {
        sender.sendMessage(parse(key, resolvers));
    }

    @Override
    public void sendRawMessage(CommandSender sender, String message) {
        sender.sendMessage(miniMessage.deserialize(message));
    }
}
