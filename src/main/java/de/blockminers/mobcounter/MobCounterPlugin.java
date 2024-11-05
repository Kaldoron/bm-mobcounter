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

    @Override
    public void onEnable() {
    	createCustomConfig();  // Load or create the config file on startup
        getLogger().info("MobCounterPlugin ist aktiviert!");
        // Registriert den Befehl /mobs
        this.getCommand("mobs").setExecutor(new MobCommandExecutor());
    }

    @Override
    public void onDisable() {
        getLogger().info("MobCounterPlugin ist deaktiviert!");
    }
    public void createCustomConfig() {
        configFile = new File(getDataFolder(), "config.yml");

        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs(); // Create the directory if it doesn't exist
            saveResource("config.yml", false); // Copy the default config.yml from the plugin JAR
        }

        config = YamlConfiguration.loadConfiguration(configFile); // Load the config file
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
                sender.sendMessage("Dieser Befehl kann nur von einem Spieler verwendet werden.");
                return true;
            }

            Player player = (Player) sender;
            Map<EntityType, Integer> mobCounts = new HashMap<>();

            // Scannt alle Entitys im Umkreis von 128 Blöcken
            for (Entity entity : player.getNearbyEntities(128, 128, 128)) {
                EntityType type = entity.getType();
                mobCounts.put(type, mobCounts.getOrDefault(type, 0) + 1);
            }

            player.sendMessage(ChatColor.GOLD + "[" + ChatColor.RED + "Mobcounter" + ChatColor.GOLD + "] " + ChatColor.GOLD + "Entitys in einem Radius von 128 Blöcken:");

         // Listen für jede Gruppe vorbereiten
            List<String> gruppe1 = new ArrayList<>();
            List<String> gruppe2 = new ArrayList<>();
            List<String> gruppe3 = new ArrayList<>();

            // Entities nach Gruppen sortieren und formatierte Einträge in die Listen hinzufügen
            for (Map.Entry<EntityType, Integer> entry : mobCounts.entrySet()) {
                EntityType type = entry.getKey();
                int count = entry.getValue();

                // Gruppe 1: Armorstands, Blockdisplays, Itemdisplays und Itemframes (Rot)
                if (type == EntityType.ARMOR_STAND || type == EntityType.BLOCK_DISPLAY || 
                    type == EntityType.ITEM_DISPLAY || type == EntityType.ITEM_FRAME) {
                    gruppe1.add(ChatColor.RED + "- " + type.name() + ": " + count);
                }
                // Gruppe 2: Tiere und Monster (Grün)
                else if (type == EntityType.COW || type == EntityType.SHEEP || type == EntityType.PIG || 
                         type == EntityType.CHICKEN || type == EntityType.ZOMBIE || type == EntityType.SKELETON || 
                         type == EntityType.CREEPER || type == EntityType.SPIDER || type == EntityType.ENDERMAN) {
                    gruppe2.add(ChatColor.GREEN + "- " + type.name() + ": " + count);
                }
                // Gruppe 3: Alle anderen Entitys (Gelb)
                else {
                    gruppe3.add(ChatColor.YELLOW + "- " + type.name() + ": " + count);
                }
            }

            // Ausgaben der Gruppen in der gewünschten Reihenfolge
            player.sendMessage(ChatColor.GOLD + "Gruppe 1: Armorstands, Blockdisplays, Itemdisplays, Itemframes");
            for (String entry : gruppe1) {
                player.sendMessage(entry);
            }

            player.sendMessage(ChatColor.GOLD + "Gruppe 2: Tiere und Monster");
            for (String entry : gruppe2) {
                player.sendMessage(entry);
            }

            player.sendMessage(ChatColor.GOLD + "Gruppe 3: Andere Entitys");
            for (String entry : gruppe3) {
                player.sendMessage(entry);
            }


            return true;
        }
    }
}
