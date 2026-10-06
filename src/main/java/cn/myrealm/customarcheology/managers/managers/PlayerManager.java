package cn.myrealm.customarcheology.managers.managers;

import cn.myrealm.customarcheology.enums.Config;
import cn.myrealm.customarcheology.enums.Messages;
import cn.myrealm.customarcheology.managers.BaseManager;
import cn.myrealm.customarcheology.mechanics.cores.ArcheologyInstance;
import cn.myrealm.customarcheology.mechanics.PlayerLookAt;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author rzt1020
 */
public class PlayerManager extends BaseManager {
    private static PlayerManager instance;
    private Map<Player, PlayerLookAt> playerLookAtMap;
    private Map<Player, ArcheologyInstance> playerBlockMap;

    public PlayerManager(JavaPlugin plugin) {
        super(plugin);
        instance = this;
    }

    @Override
    protected void onInit() {
        playerBlockMap = new HashMap<>(5);
        playerLookAtMap = new HashMap<>(5);
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            playerLookAtMap.put(player, new PlayerLookAt(player));
        }
    }

    @Override
    protected void onDisable() {
        for (PlayerLookAt playerLookAt : playerLookAtMap.values()) {
            playerLookAt.cancelTask();
        }
    }

    public static PlayerManager getInstance() {
        return instance;
    }

    public void playerJoin(Player player) {
        playerLookAtMap.put(player, new PlayerLookAt(player));
    }

    public void playerQuit(Player player) {
        cancelBrush(player);
        if (playerLookAtMap.containsKey(player)) {
            playerLookAtMap.get(player).cancelTask();
            playerLookAtMap.remove(player);
        }
    }

    public void setBrush(Player player, ArcheologyInstance archeologyInstance, BlockFace blockFace, ItemStack tool) {
        if (playerBlockMap.containsValue(archeologyInstance)) {
            if (playerBlockMap.get(player) == null || !playerBlockMap.get(player).equals(archeologyInstance)) {
                player.sendMessage(Messages.GAME_ALREADY_BRUSHING.getMessageWithPrefix());
            }
            if (Config.DEBUG.asBoolean()) {
                Bukkit.getConsoleSender().sendMessage("§x§9§8§F§B§9§8[CustomArcheology] §fFound same block.");
            }
            return;
        }
        if (playerBlockMap.containsKey(player)) {
            PlayerManager playerManager = PlayerManager.getInstance();
            playerManager.cancelBrush(player);
            if (Config.DEBUG.asBoolean()) {
                Bukkit.getConsoleSender().sendMessage("§x§9§8§F§B§9§8[CustomArcheology] §fFound same player.");
            }
            return;
        }
        playerBlockMap.put(player, archeologyInstance);
        playerLookAtMap.get(player).setTask(new BukkitRunnable() {
            @Override
            public void run() {
            if (Config.DEBUG.asBoolean()) {
                Bukkit.getConsoleSender().sendMessage("§x§9§8§F§B§9§8[CustomArcheology] §fCancel brush.");
            }
            cancelBrush(player);
            }
        });
        archeologyInstance.play(blockFace, tool);
    }

    public void cancelBrush(Player player) {
        if (playerBlockMap.containsKey(player)) {
            playerBlockMap.get(player).pause();
            playerBlockMap.remove(player);
        }
    }

    public void cancelBlock(ArcheologyInstance block) {
        playerBlockMap.entrySet().removeIf(entry -> entry.getValue() == block);
    }
}
