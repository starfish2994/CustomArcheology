package cn.myrealm.customarcheology.listeners.bukkit;

import cn.myrealm.customarcheology.listeners.BaseListener;
import cn.myrealm.customarcheology.managers.managers.ChunkManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class BlockLifecycleListener extends BaseListener {

    public BlockLifecycleListener(JavaPlugin plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent event) {
        ChunkManager.getInstance().unloadChunk(event.getChunk());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        // A new placement takes ownership even if its state equals the old cooldown block.
        ChunkManager.getInstance().getPersistentDataChunk(event.getBlock().getLocation())
                .unregisterBlock(event.getBlock().getLocation());
    }
}
