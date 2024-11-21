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

            // Standard-Scanradius aus der Konfiguration holen
            int maxRadius = getConfig().getInt("scan-radius", 128);
            int radius = maxRadius; // Standardradius ist der maximale Radius

            // Falls der Benutzer einen Radius angibt, validiere den Wert
            if (args.length > 0) {
                try {
                    radius = Integer.parseInt(args[0]); // Benutzerwert parsen
                    if (radius < 1 || radius > maxRadius) { // Gültigkeit prüfen
                        player.sendMessage(ChatColor.RED + "Bitte gib einen Radius zwischen 1 und " + maxRadius + " an.");
                        return true;
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Bitte gib eine gültige Zahl für den Radius an.");
                    return true;
                }
            }

            // Scannt alle Entities im spezifizierten Radius
            for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                EntityType type = entity.getType();
                mobCounts.put(type, mobCounts.getOrDefault(type, 0) + 1);
            }

            // Header aus der Konfiguration anzeigen und den Scan-Radius hinzufügen
            String headerMessage = getConfig().getString("header", "Entity-Zähler im Radius von %radius% Blöcken:")
                    .replace("%radius%", String.valueOf(radius)); // %radius% durch den tatsächlichen Radius ersetzen
            player.sendMessage(ChatColor.GOLD + headerMessage); // Header anzeigen

            // Jede Gruppe aus der Konfiguration durchgehen und Entitys entsprechend zuordnen
            for (String groupKey : getConfig().getConfigurationSection("gruppen").getKeys(false)) {
                // Name und Farbe der Gruppe aus der Konfiguration laden
                String groupName = getConfig().getString("gruppen." + groupKey + ".name");
                String colorCode = getConfig().getString("gruppen." + groupKey + ".color", "WHITE"); // Standardfarbe, falls nicht definiert
                ChatColor color = ChatColor.valueOf(colorCode.toUpperCase()); // String in ChatColor umwandeln

                List<String> entitiesList = getConfig().getStringList("gruppen." + groupKey + ".entities");
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
