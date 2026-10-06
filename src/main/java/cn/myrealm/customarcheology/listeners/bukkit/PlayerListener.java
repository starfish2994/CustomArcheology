package cn.myrealm.customarcheology.listeners.bukkit;

import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

import cn.myrealm.customarcheology.listeners.BaseListener;
import cn.myrealm.customarcheology.managers.managers.PlayerManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author rzt1020
 */
public class PlayerListener extends BaseListener {
    public PlayerListener(JavaPlugin plugin) {
        super(plugin);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        PlayerManager playerManager = PlayerManager.getInstance();
        playerManager.playerJoin(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        PlayerManager playerManager = PlayerManager.getInstance();
        playerManager.playerQuit(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onHeldItemChange(PlayerItemHeldEvent event) {
        PlayerManager.getInstance().cancelBrush(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        PlayerManager.getInstance().cancelBrush(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onDropItem(PlayerDropItemEvent event) {
        PlayerManager.getInstance().cancelBrush(event.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        PlayerManager.getInstance().cancelBrush(event.getEntity());
    }

    @EventHandler
    public void onChangeWorld(PlayerChangedWorldEvent event) {
        PlayerManager.getInstance().cancelBrush(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof org.bukkit.entity.Player player) {
            PlayerManager.getInstance().cancelBrush(player);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof org.bukkit.entity.Player player) {
            PlayerManager.getInstance().cancelBrush(player);
        }
    }
}
