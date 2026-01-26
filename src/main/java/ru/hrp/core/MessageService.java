package ru.hrp.core;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import ru.hrp.config.ConfigService;

public interface MessageService {
    Component parse(String key, TagResolver... resolvers);
    void sendMessage(CommandSender sender, String key, TagResolver... resolvers);
    void sendRawMessage(CommandSender sender, String message);
}
