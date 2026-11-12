package com.enderdevelopment.plugin;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {
    private BuffManager buffManager;
    private EchoListener echoListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        buffManager = new BuffManager(this);
        echoListener = new EchoListener(this, buffManager);
        getServer().getPluginManager().registerEvents(echoListener, this);

        PluginCommand command = getCommand("echo");
        if (command == null) {
            getLogger().severe("The echo command is missing from plugin.yml.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        EchoCommand echoCommand = new EchoCommand(this, buffManager);
        command.setExecutor(echoCommand);
        command.setTabCompleter(echoCommand);
    }

    @Override
    public void onDisable() {
        if (echoListener != null) {
            echoListener.shutdown();
        }
    }

    void reloadSettings() {
        reloadConfig();
        buffManager.reloadFromConfig();
    }
}
