package com.enderdevelopment.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

final class EchoCommand implements CommandExecutor, TabCompleter {
    private final Main plugin;
    private final BuffManager buffManager;

    EchoCommand(Main plugin, BuffManager buffManager) {
        this.plugin = plugin;
        this.buffManager = buffManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("echo.admin")) {
            sender.sendMessage(Component.text("You do not have permission to manage Echo buffs.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("buffs")) {
            sender.sendMessage(Component.text("Echo buffs:", NamedTextColor.GOLD));
            for (BuffManager.Buff buff : buffManager.getBuffs()) {
                NamedTextColor color = buff.enabled ? NamedTextColor.GREEN : NamedTextColor.GRAY;
                sender.sendMessage(Component.text("- " + buff.id + " [" + (buff.enabled ? "enabled" : "disabled") + "]", color));
            }
            return true;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("buff")) {
            String state = args[2].toLowerCase(Locale.ROOT);
            if (!state.equals("on") && !state.equals("off")) {
                sendUsage(sender);
                return true;
            }
            boolean enabled = state.equals("on");
            if (!buffManager.setEnabled(args[1], enabled)) {
                sender.sendMessage(Component.text("Unknown buff: " + args[1], NamedTextColor.RED));
                return true;
            }
            sender.sendMessage(Component.text("Buff " + args[1].toLowerCase(Locale.ROOT) + " is now " + (enabled ? "enabled" : "disabled") + ".", NamedTextColor.GREEN));
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadSettings();
            sender.sendMessage(Component.text("Echo settings reloaded.", NamedTextColor.GREEN));
            return true;
        }

        sendUsage(sender);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("echo.admin")) {
            return List.of();
        }
        if (args.length == 1) {
            return filter(List.of("buffs", "buff", "reload"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("buff")) {
            List<String> ids = new ArrayList<>();
            for (BuffManager.Buff buff : buffManager.getBuffs()) {
                ids.add(buff.id);
            }
            return filter(ids, args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("buff")) {
            return filter(List.of("on", "off"), args[2]);
        }
        return List.of();
    }

    private static List<String> filter(List<String> options, String prefix) {
        String normalized = prefix.toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.startsWith(normalized)).toList();
    }

    private static void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage: /echo buffs | /echo buff <id> <on|off> | /echo reload", NamedTextColor.YELLOW));
    }
}
