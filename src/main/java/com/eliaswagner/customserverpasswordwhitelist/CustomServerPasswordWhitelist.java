package com.eliaswagner.customserverpasswordwhitelist;

import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.List;


public final class CustomServerPasswordWhitelist extends JavaPlugin implements Listener {
    private FileConfiguration approved;
    private File approvedFile;
    private List<String> approvedList;
    private String password;
    private List<String> commandList;
    private Boolean isExtensionInstalled = false;

    public CustomServerPasswordWhitelist() {
    }

    public void onEnable() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            /*
             * We register the EventListener here, when PlaceholderAPI is installed.
             * Since all events are in the main class (this class), we simply use "this"
             */
            Bukkit.getConsoleSender().sendMessage("[CustomServerPasswordWhitelist] Found PlaceholderAPI installed.");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"papi ecloud download Player");
            isExtensionInstalled = PlaceholderAPI.isRegistered("Player");
        } else {
            /*
             * We inform about the fact that PlaceholderAPI isn't installed and then
             * disable this plugin to prevent issues.
             */
            getLogger().warning("PlaceholderAPI not found. PlaceholderAPI is required.");
            Bukkit.getPluginManager().disablePlugin(this);
        }



        if (!this.getDataFolder().exists()) {
            this.getDataFolder().mkdirs();
        }

        this.approvedFile = new File(this.getDataFolder(), "approved.yml");
        if (!this.approvedFile.exists()) {
            try {
                this.approvedFile.createNewFile();
            } catch (IOException var2) {
                var2.printStackTrace();
            }
        }

        this.approved = YamlConfiguration.loadConfiguration(this.approvedFile);
        this.approvedList = this.approved.getStringList("approved");
        this.saveDefaultConfig();
        if ((this.password = this.getConfig().getString("password")) == null) {
            this.password = "entry";
        }
        this.commandList = this.getConfig().getStringList("commands-after-first-join");

        Bukkit.getPluginManager().registerEvents(this, this);
    }

    public void onDisable() {
        this.saveApproved();
    }

    private void saveApproved() {
        this.approved.set("approved", this.approvedList);

        try {
            this.approved.save(this.approvedFile);
        } catch (IOException var2) {
            var2.printStackTrace();
        }

    }

    private void runCommands(Player p) {
        getServer().getScheduler().scheduleSyncDelayedTask(this, () -> {
            isExtensionInstalled = PlaceholderAPI.isRegistered("Player");
            if(!isExtensionInstalled){
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"papi ecloud download Player");
            }
            for (String command: commandList) {
                String c = PlaceholderAPI.setPlaceholders(p,command);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),c);
            }
        });
    }


    private boolean approved(Player p) {
        return this.approved(p, true);
    }

    private boolean approved(Player p, boolean sendMsg) {
        String player = p.getUniqueId().toString();
        if (this.approvedList.contains(player)) {
            return true;
        } else {
            if (sendMsg) {
                p.sendMessage(ChatColor.RED + "Gib das Server-Passwort ein:");
            }

            return false;
        }
    }

    @EventHandler
    public void joinManager(PlayerJoinEvent e) {
        if (!this.approved(e.getPlayer())) {
            e.getPlayer().setGameMode(GameMode.ADVENTURE);
        }

    }

    @EventHandler
    public void checkForPassword(AsyncPlayerChatEvent e) {
        if(!this.approved(e.getPlayer(), false)){
            //check pw
            if(e.getMessage().equals(this.password)) {
                this.approvedList.add(e.getPlayer().getUniqueId().toString());
                e.getPlayer().sendMessage(ChatColor.GREEN + "Richtig. Du stehst jetzt auf der Whitelist.");
                e.setCancelled(true);
                Bukkit.getScheduler().runTask(this, () -> {
                    e.getPlayer().setGameMode(Bukkit.getServer().getDefaultGameMode());
                });
                this.saveApproved();
                this.runCommands(e.getPlayer());
            }
            else {
                e.setCancelled(true);
            }
        }else {
            //print chat
        }
    }

    @EventHandler
    public void stopInteract(PlayerInteractEvent e) {
        if (!this.approved(e.getPlayer())) {
            e.setCancelled(true);
        }

    }

    @EventHandler
    public void stopDrop(PlayerDropItemEvent e) {
        if (!this.approved(e.getPlayer())) {
            e.setCancelled(true);
        }

    }

    @EventHandler
    public void stopPickup(PlayerPickupItemEvent e) {
        if (!this.approved(e.getPlayer())) {
            e.setCancelled(true);
        }

    }

    @EventHandler
    public void stopEntityTarget(EntityTargetEvent e) {
        if (e.getTarget() instanceof Player) {
            Player p = (Player)e.getTarget();
            if (!this.approved(p)) {
                e.setCancelled(true);
            }

        }
    }

    @EventHandler
    public void stopAttack(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player) {
            Player p = (Player)e.getDamager();
            if (!this.approved(p)) {
                e.setCancelled(true);
            }

        }
    }

    @EventHandler
    public void disallowCommands(PlayerCommandPreprocessEvent e) {
        if (!this.approved(e.getPlayer())) {
            e.setCancelled(true);
        }

    }
}
