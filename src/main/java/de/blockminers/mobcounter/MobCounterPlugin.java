package de.blockminers.mobcounter;

import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MobCounterPlugin extends JavaPlugin {
	private FileConfiguration config;
    private File configFile;
    private final int currentConfigVersion = 2; // Set your current config version here

    @Override
    public void onEnable() {
    	createCustomConfig();  // Load or create the config file on startup
        getLogger().info("MobCounterPlugin is active!");
        // Register the Command /mobs
        this.getCommand("mobs").setExecutor(new MobCommandExecutor());
    }

    @Override
    public void onDisable() {
        getLogger().info("MobCounterPlugin is inactive!");
    }
    public void createCustomConfig() {
        configFile = new File(getDataFolder(), "config.yml");

        if (configFile.exists()) {
            // Load the existing config to check the version
            config = YamlConfiguration.loadConfiguration(configFile);

            int existingConfigVersion = config.getInt("version", 0); // Default to 0 if version is not found

            // Check if the existing config version is outdated
            if (existingConfigVersion < currentConfigVersion) {
                // Rename the old config file
                File oldConfigFile = new File(getDataFolder(), "config_old.yml");
                if (configFile.renameTo(oldConfigFile)) {
                    getLogger().info("Old config.yml renamed to config_old.yml due to version mismatch.");
                } else {
                    getLogger().warning("Failed to rename old config.yml. Loading the old config instead.");
                }
                // Copy the new default config.yml from the plugin JAR
                saveResource("config.yml", false);
                config = YamlConfiguration.loadConfiguration(configFile); // Load the new config
            }
        } else {
            // If no config exists, copy the default config.yml from the plugin JAR
            configFile.getParentFile().mkdirs();
            saveResource("config.yml", false);
            config = YamlConfiguration.loadConfiguration(configFile); // Load the config file
        }
    }

    public FileConfiguration getConfig() {
        if (config == null) {
            createCustomConfig();
        }
        return config;
    }

    public void saveConfig() {
        if (config == null || configFile == null) return;

        try {
            getConfig().save(configFile); // Save any changes back to config.yml
        } catch (IOException e) {
            getLogger().severe("Could not save config to " + configFile);
        }
    }

    public void reloadConfig() {
        if (configFile == null) {
            configFile = new File(getDataFolder(), "config.yml");
        }
        config = YamlConfiguration.loadConfiguration(configFile); // Reload config from file
    }

    private class MobCommandExecutor implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("This Command can only be performed by a Player.");
                return true;
            }

            Player player = (Player) sender;
            Map<EntityType, Integer> mobCounts = new HashMap<>();

            // Read scan-radius from config file
            int radius = getConfig().getInt("scan-radius", 128);

            // Scan all Entitys in the Area set in the Radius
            for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                EntityType type = entity.getType();
                mobCounts.put(type, mobCounts.getOrDefault(type, 0) + 1);
            }

         // Show Header from Config and Import the Radius
            String headerMessage = getConfig().getString("header", "Entity-Zähler im Radius von %radius% Blöcken:")
                                        .replace("%radius%", String.valueOf(radius));  // %radius% durch den tatsächlichen Radius ersetzen
            player.sendMessage(ChatColor.GOLD + headerMessage);  // Header anzeigen

            // Go through every Entry in Config and get the entities
            for (String groupKey : getConfig().getConfigurationSection("group").getKeys(false)) {
                // Name und Farbe der Gruppe aus der Konfiguration laden
                String groupName = getConfig().getString("group." + groupKey + ".name");
                String colorCode = getConfig().getString("group." + groupKey + ".color", "WHITE"); // Standardfarbe, falls nicht definiert
                ChatColor color = ChatColor.valueOf(colorCode.toUpperCase()); // String in ChatColor umwandeln

                List<String> entitiesList = getConfig().getStringList("group." + groupKey + ".entities");
                List<String> groupMessages = new ArrayList<>();

                // Entity-Zahlen für die Gruppe sammeln
                for (Map.Entry<EntityType, Integer> entry : mobCounts.entrySet()) {
                    EntityType type = entry.getKey();
                    int count = entry.getValue();

                    if (entitiesList.contains(type.name())) {
                        groupMessages.add(color + "- " + type.name() + ": " + count);
                    }
                }

                // Gruppennachricht ausgeben, wenn es Einträge gibt
                if (!groupMessages.isEmpty()) {
                    player.sendMessage(ChatColor.GOLD + groupName + ":"); // Gruppennamen anzeigen
                    for (String entry : groupMessages) {
                        player.sendMessage(entry); // Entity-Details anzeigen
                    }
                }
            }

            return true;
        }
    }
}
