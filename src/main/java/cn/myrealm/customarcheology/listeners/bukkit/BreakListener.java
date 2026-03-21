package cn.myrealm.customarcheology.listeners.bukkit;


import cn.myrealm.customarcheology.enums.Config;
import cn.myrealm.customarcheology.listeners.BaseListener;
import cn.myrealm.customarcheology.managers.managers.ChunkManager;
import cn.myrealm.customarcheology.mechanics.cores.ArcheologyBlock;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

import static org.bukkit.Material.BARRIER;

/**
 * @author rzt1020
 */
public class BreakListener extends BaseListener {
    private enum BreakAction {
        RESPAWN_IF_AVAILABLE
    }

    public BreakListener(JavaPlugin plugin) {
        super(plugin);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreakBlock(BlockBreakEvent event) {
        Location loc = event.getBlock().getLocation();
        ChunkManager chunkManager = ChunkManager.getInstance();
        boolean creative = event.getPlayer().getGameMode().equals(GameMode.CREATIVE);
        if (chunkManager.isRespawningBlock(loc)) {
            if (creative) {
                chunkManager.unregisterBlock(loc);
            } else {
                event.setCancelled(true);
            }
            return;
        }
        if (creative) {
            if (chunkManager.isArcheologyBlock(loc)) {
                chunkManager.unregisterBlock(loc);
            }
            return;
        }
        if (handleBrokenArcheologyBlock(chunkManager, loc, Config.DISAPPEAR_AFTER_BREAK.asBoolean(), BreakAction.RESPAWN_IF_AVAILABLE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreakByTnt(EntityExplodeEvent event) {
        ChunkManager chunkManager = ChunkManager.getInstance();
        event.blockList().removeIf(block -> shouldBlockStay(chunkManager, block.getLocation()));
        for (Block block : event.blockList()) {
            Location loc = block.getLocation();
            if (chunkManager.isArcheologyBlock(loc)) {
                chunkManager.removeBlock(loc);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        ChunkManager chunkManager = ChunkManager.getInstance();
        event.blockList().removeIf(block -> shouldBlockStay(chunkManager, block.getLocation()));
        for (Block block : event.blockList()) {
            Location loc = block.getLocation();
            if (chunkManager.isArcheologyBlock(loc)) {
                chunkManager.removeBlock(loc);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityGrief(EntityChangeBlockEvent event) {
        Location loc = event.getBlock().getLocation();
        ChunkManager chunkManager = ChunkManager.getInstance();
        if (chunkManager.isRespawningBlock(loc)) {
            event.setCancelled(true);
            return;
        }
        if (handleBrokenArcheologyBlock(chunkManager, loc, false, BreakAction.RESPAWN_IF_AVAILABLE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onRightClick(PlayerInteractEvent event) {
        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        Location loc = Objects.requireNonNull(event.getClickedBlock()).getLocation();
        ChunkManager chunkManager = ChunkManager.getInstance();
        ArcheologyBlock block = chunkManager.getArcheologyBlock(loc);
        if (block != null) {
            loc.getBlock().setType(BARRIER);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onLeftClick(PlayerInteractEvent event) {
        if (!event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
            return;
        }
        if (!event.getPlayer().getGameMode().equals(GameMode.SURVIVAL) && !event.getPlayer().getGameMode().equals(GameMode.CREATIVE)) {
            return;
        }
        Location loc = Objects.requireNonNull(event.getClickedBlock()).getLocation();
        ChunkManager chunkManager = ChunkManager.getInstance();
        if (chunkManager.isRespawningBlock(loc)) {
            if (!event.getPlayer().getGameMode().equals(GameMode.CREATIVE)) {
                event.setCancelled(true);
            }
            return;
        }
        ArcheologyBlock block = chunkManager.getArcheologyBlock(loc);
        if (block != null) {
            if (event.getPlayer().getGameMode().equals(GameMode.CREATIVE)) {
                return;
            }
            event.setCancelled(true);
            event.getPlayer().playSound(event.getPlayer(), block.getBrushSound(), 1, 1);
            handleBrokenArcheologyBlock(chunkManager, loc, Config.DISAPPEAR_AFTER_BREAK.asBoolean(), BreakAction.RESPAWN_IF_AVAILABLE);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonMove(BlockPistonExtendEvent event) {
        ChunkManager chunkManager = ChunkManager.getInstance();
        if (event.getBlocks().stream().anyMatch(block -> chunkManager.isManagedBlock(block.getLocation()) || chunkManager.isRespawningBlock(block.getLocation()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        ChunkManager chunkManager = ChunkManager.getInstance();
        if (event.getBlocks().stream().anyMatch(block -> chunkManager.isManagedBlock(block.getLocation()) || chunkManager.isRespawningBlock(block.getLocation()))) {
            event.setCancelled(true);
        }
    }

    private boolean shouldBlockStay(ChunkManager chunkManager, Location loc) {
        if (chunkManager.isRespawningBlock(loc)) {
            return true;
        }
        return handleBrokenArcheologyBlock(chunkManager, loc, false, BreakAction.RESPAWN_IF_AVAILABLE);
    }

    private boolean handleBrokenArcheologyBlock(ChunkManager chunkManager, Location loc, boolean disappearAfterBreak, BreakAction action) {
        ArcheologyBlock block = chunkManager.getArcheologyBlock(loc);
        if (block == null) {
            return false;
        }
        if (action.equals(BreakAction.RESPAWN_IF_AVAILABLE) && block.shouldRespawn()) {
            chunkManager.startRespawnCooldown(loc);
            return true;
        }
        chunkManager.removeBlock(loc);
        if (disappearAfterBreak) {
            loc.getBlock().setType(Material.AIR);
        }
        return true;
    }
}
