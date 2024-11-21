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

    private static final int CURRENT_CONFIG_VERSION = 2; // Update this as needed for your plugin

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

        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs(); // Create the directory if it doesn't exist
            saveResource("config.yml", false); // Copy the default config.yml from the plugin JAR
        }

        config = YamlConfiguration.loadConfiguration(configFile); // Load the config file
    }

    public void checkConfigVersion() {
        int configVersion = getConfig().getInt("version", 0); // Default to 0 if not present

        if (configVersion < CURRENT_CONFIG_VERSION) {
            getLogger().warning("Config version " + configVersion +
                    " is outdated. Updating to version " + CURRENT_CONFIG_VERSION + "...");
            handleOutdatedConfig(); // Call your existing routine to handle outdated config
            getConfig().set("version", CURRENT_CONFIG_VERSION); // Update version in config
            saveConfig();
        }
    }

    public void handleOutdatedConfig() {
        // Example update logic: Add new config keys or values if they are missing
        if (!getConfig().contains("no-entities-found")) {
            getConfig().set("no-entities-found", "No Entities in %radius% Blocks could be found.");
        }

        // Add other update logic here if necessary...
        getLogger().info("Config successfully updated.");
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

    private class ReloadCommandExecutor implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!sender.hasPermission("mobcounterplugin.reload")) {
                sender.sendMessage(ChatColor.RED + "You dont have Permission to perform this Command.");
                return true;
            }

            reloadConfig();
            checkConfigVersion(); // Ensure the updated config version matches the current
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
            int maxRadius = getConfig().getInt("scan-radius", 128);
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
            
           // If no Entities have been found
            if (mobCounts.isEmpty()) {
                String noEntitiesMessage = getConfig().getString("no-entities-found",
                        "No Entities in Radius of %radius%  Blocks could be found..")
                        .replace("%radius%", String.valueOf(radius));
                player.sendMessage(ChatColor.RED + noEntitiesMessage);
                return true; // Exit early
            }

            // Show header from config with radius 
            String rawHeaderMessage = getConfig().getString("header", "&6Entity-Counter in Radius of %radius% Blocks:");
            String formattedHeaderMessage = ChatColor.translateAlternateColorCodes('&', rawHeaderMessage.replace("%radius%", String.valueOf(radius)));
            player.sendMessage(formattedHeaderMessage); // Send formatted header message
            
            // Go through every Group within the List
            for (String groupKey : getConfig().getConfigurationSection("groups").getKeys(false)) {
                // Load Color and Name from Config
                String groupName = getConfig().getString("groups." + groupKey + ".name");
                String colorCode = getConfig().getString("groups." + groupKey + ".color", "&f"); // Standardcolor: Weiß (&f)
                String translatedColorCode = ChatColor.translateAlternateColorCodes('&', colorCode); // Minecraft-Colorcodes translation
                ChatColor color = ChatColor.valueOf(translatedColorCode.toUpperCase().replace("§", ""));

                List<String> entitiesList = getConfig().getStringList("groups." + groupKey + ".entities");
                
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
