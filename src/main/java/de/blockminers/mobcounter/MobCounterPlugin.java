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

    private static final int CURRENT_CONFIG_VERSION = 3; // Update this as needed for your plugin

    @Override
    public void onEnable() {
        createCustomConfig(); // Load or create the config file on startup
        checkConfigVersion(); // Check the version and handle outdated configs
        getLogger().info("MobCounterPlugin is activated!");
        // Register the /mcpmobs command
        // Register the /mcpreload command
        this.getCommand("mcpmobs").setExecutor(new MobCommandExecutor());
        this.getCommand("mcpreload").setExecutor(new ReloadCommandExecutor());
    }

    @Override
    public void onDisable() {
        getLogger().info("MobCounterPlugin is deactivated!");
    }

    public void createCustomConfig() {
        configFile = new File(getDataFolder(), "config.yml");

        if (configFile.exists()) {
            // Load the existing config to check the version
            config = YamlConfiguration.loadConfiguration(configFile);
            int existingConfigVersion = config.getInt("version", 0); // Default to 0 if version is not found

            // Check if the existing config version is outdated
            if (existingConfigVersion < CURRENT_CONFIG_VERSION) {
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

    public void reloadConfig() {
        if (configFile == null) {
            configFile = new File(getDataFolder(), "config.yml");
        }
        config = YamlConfiguration.loadConfiguration(configFile); // Reload config from file
    }

    private void checkConfigVersion() {
        int existingConfigVersion = config.getInt("version", 0); // Default to 0 if version is not found

        if (existingConfigVersion < CURRENT_CONFIG_VERSION) {
            // Logic to handle version mismatch (already done in createCustomConfig)
            getLogger().info("Config file version is outdated, updating...");
        }
    }
    
    private class ReloadCommandExecutor implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!sender.hasPermission("mobcounterplugin.reload")) {
                sender.sendMessage(ChatColor.RED + "You dont have Permission to perform this Command.");
                return true;
            }

            reloadConfig();
            sender.sendMessage(ChatColor.GREEN + "Config successfully reloaded.");
            return true;
        }
    }
    
    private class MobCommandExecutor implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("This Command can only be used by a Player.");
                return true;
            }
            
            Player player = (Player) sender;
            Map<EntityType, Integer> mobCounts = new HashMap<>();

            // Get scan-radius from Config
            int maxRadius = config.getInt("scan-radius", 128);
            int radius = maxRadius; // Standardradius is max Radius

            // If Player gives Radius, validate
            if (args.length > 0) {
                try {
                    radius = Integer.parseInt(args[0]); // Uservalue parsing
                    if (radius < 1 || radius > maxRadius) { // Validate
                        player.sendMessage(ChatColor.RED + "Enter a Value between 1 and " + maxRadius + " .");
                        return true;
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Enter a Valid Value for Radius.");
                    return true;
                }
            }

            // Scans Entities in Radius
            for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                EntityType type = entity.getType();
                mobCounts.put(type, mobCounts.getOrDefault(type, 0) + 1);
            }
            
           /// Check if no entities were found
            if (mobCounts.isEmpty()) {
                // Fetch the message from the config with a default fallback
                String rawNoEntitiesMessage = config.getString(
                        "no-entities-found",
                        "No Entities in Radius of %radius% Blocks could be found.."
                );

                // Replace the %radius% placeholder and apply color codes
                String formattedNoEntitiesMessage = ChatColor.translateAlternateColorCodes('&', rawNoEntitiesMessage.replace("%radius%", String.valueOf(radius)));

                // Send the formatted message to the player
                player.sendMessage(formattedNoEntitiesMessage);

                return true; // Exit early
            }

            // Show header from config with radius 
            String rawHeaderMessage = config.getString("header", "Entity-Counter in Radius of %radius% Blocks:");
            String formattedHeaderMessage = ChatColor.translateAlternateColorCodes('&', rawHeaderMessage.replace("%radius%", String.valueOf(radius)));
            player.sendMessage(formattedHeaderMessage); // Send formatted header message
            
            // Go through every Group within the List
            for (String groupKey : config.getConfigurationSection("groups").getKeys(false)) {
                // Load Color and Name from Config
                String groupName = config.getString("groups." + groupKey + ".name");
                String colorCode = config.getString("groups." + groupKey + ".color", "&f"); // Standardcolor: Weiß (&f)
                String translatedColorCode = ChatColor.translateAlternateColorCodes('&', colorCode); // Minecraft-Colorcodes translation
                ChatColor color = ChatColor.valueOf(translatedColorCode.toUpperCase().replace("§", ""));

                List<String> entitiesList = config.getStringList("groups." + groupKey + ".entities");
                
                // Declare groupEntities inside the loop
                List<Map.Entry<EntityType, Integer>> groupEntities = new ArrayList<>();

                // Get Entitycount for Group
                for (Map.Entry<EntityType, Integer> entry : mobCounts.entrySet()) {
                    EntityType type = entry.getKey();
                    int count = entry.getValue();

                    if (entitiesList.contains(type.name())) {
                        groupEntities.add(entry);
                    }
                }

                // Sort the group entities by count (descending)
                groupEntities.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

                // Message if Entitys found
                if (!groupEntities.isEmpty()) {
                    player.sendMessage(ChatColor.GOLD + groupName + ":"); // Show Groupname
                    for (Map.Entry<EntityType, Integer> entry : groupEntities) {
                        String formattedName = formatEntityName(entry.getKey().name());
                        int count = entry.getValue();
                        player.sendMessage(color + "- " + formattedName + ": " + count);
                    }
                }
            }

            return true;
        }
     // Utility method to format the Bukkit entity name for better readability
        private String formatEntityName(String bukkitName) {
            String[] words = bukkitName.toLowerCase().split("_");
            StringBuilder formattedName = new StringBuilder();

            for (String word : words) {
                formattedName.append(Character.toUpperCase(word.charAt(0)))
                              .append(word.substring(1))
                              .append(" ");
            }

            return formattedName.toString().trim();
        }
    }
}
