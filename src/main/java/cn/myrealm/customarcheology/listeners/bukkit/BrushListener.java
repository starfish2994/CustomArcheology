package cn.myrealm.customarcheology.listeners.bukkit;

import cn.myrealm.customarcheology.mechanics.BrushService;
import cn.myrealm.customarcheology.mechanics.cores.BlockMode;
import cn.myrealm.customarcheology.enums.Config;
import cn.myrealm.customarcheology.listeners.BaseListener;
import cn.myrealm.customarcheology.managers.managers.ChunkManager;
import cn.myrealm.customarcheology.managers.managers.PlayerManager;
import cn.myrealm.customarcheology.mechanics.cores.ArcheologyInstance;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author rzt1020
 */
public class BrushListener extends BaseListener {
    public BrushListener(JavaPlugin plugin) {
        super(plugin);
    }

    @EventHandler
    public void onPlayerRightClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null
                || event.getHand() != EquipmentSlot.HAND
                || event.useInteractedBlock() == org.bukkit.event.Event.Result.DENY) {
            return;
        }
        ArcheologyInstance block = ChunkManager.getInstance().getInstanceAt(event.getClickedBlock().getLocation());
        if (block != null && block.getMode() == BlockMode.LEGACY) {
            BrushService.start(event.getPlayer(), block, event.getBlockFace(), event.getItem());
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (Config.DEBUG.asBoolean()) {
            Bukkit.getConsoleSender().sendMessage("§x§9§8§F§B§9§8[CustomArcheology] §fClick entity and cancel brush.");
        }
        PlayerManager playerManager = PlayerManager.getInstance();
        playerManager.cancelBrush(event.getPlayer());
    }
}
