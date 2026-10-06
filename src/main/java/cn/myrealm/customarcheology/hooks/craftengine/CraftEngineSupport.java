package cn.myrealm.customarcheology.hooks.craftengine;

import cn.myrealm.customarcheology.CustomArcheology;
import cn.myrealm.customarcheology.enums.NamespacedKeys;
import cn.myrealm.customarcheology.managers.managers.BlockManager;
import cn.myrealm.customarcheology.managers.managers.ChunkManager;
import cn.myrealm.customarcheology.mechanics.cores.ArcheologyBlock;
import cn.myrealm.customarcheology.mechanics.cores.ArcheologyInstance;
import cn.myrealm.customarcheology.mechanics.cores.BlockMode;

import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.bukkit.api.event.CraftEngineReloadEvent;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviors;
import net.momirealms.craftengine.core.block.parser.BlockStateParser;
import net.momirealms.craftengine.core.util.Key;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public final class CraftEngineSupport implements Listener {
    private static boolean registered;
    private static boolean ready;

    public static void register() {
        if (!registered) {
            BlockBehaviors.register(
                    Key.of("customarcheology:brushable"), BrushableBlockBehavior::new);
            registered = true;
        }
    }

    public static boolean isReady() {
        return ready;
    }

    public static void enable() {
        if (!registered) {
            throw new IllegalStateException(
                    "CraftEngine behavior registration failed during onLoad");
        }
        Bukkit.getPluginManager().registerEvents(new CraftEngineSupport(), CustomArcheology.plugin);
        // CE loads resources after plugin enable. The reload event also fires on the initial load.
        if (!CraftEngineBlocks.loadedBlocks().isEmpty()) {
            ready = true;
        }
    }

    @EventHandler
    public void onCraftEngineReload(CraftEngineReloadEvent event) {
        Bukkit.getScheduler()
                .runTask(
                        CustomArcheology.plugin,
                        () -> {
                            ready = true;
                            ChunkManager chunkManager = ChunkManager.getInstance();
                            if (chunkManager != null) {
                                chunkManager.reloadLoadedChunks();
                            }
                            if (CustomArcheology.plugin.getBlockMode() == BlockMode.CRAFTENGINE) {
                                for (String id : BlockManager.getInstance().getBlocksName()) {
                                    ArcheologyBlock block = BlockManager.getInstance().getBlock(id);
                                    if (!validDefinition(block.getCraftEngineBlockId(), block)) {
                                        CustomArcheology.plugin
                                                .getLogger()
                                                .severe(
                                                        "Invalid CE archeology block: "
                                                                + id
                                                                + " -> "
                                                                + block.getCraftEngineBlockId());
                                    }
                                    if (!validReplaceDefinition(block)) {
                                        CustomArcheology.plugin.getLogger().severe(
                                                "Invalid CE replace-block: " + id + " -> "
                                                        + block.getCraftEngineReplaceBlock());
                                    }
                                }
                            }
                        });
    }

    public static boolean validDefinition(String id, ArcheologyBlock block) {
        if (!ready) {
            return false;
        }
        var definition = CraftEngineBlocks.byId(Key.of(id));
        if (definition == null) {
            return false;
        }
        var behavior = definition.defaultState().behavior().getFirst(BrushableBlockBehavior.class);
        if (behavior == null || !behavior.archeologyId().equals(block.getName())) {
            return false;
        }
        for (int stage = 0; stage <= block.getStates().size(); stage++) {
            if (!behavior.progress().possibleValues().contains(stage)) {
                return false;
            }
        }
        String finished = block.getCraftEngineFinishedBlock();
        var finishedDefinition = CraftEngineBlocks.byId(Key.of(finished));
        if (finishedDefinition != null) {
            return finishedDefinition
                            .defaultState()
                            .behavior()
                            .getFirst(BrushableBlockBehavior.class)
                    == null;
        }
        try {
            Bukkit.createBlockData(finished);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public static ArcheologyInstance attach(Location location) {
        if (!ready) {
            return null;
        }
        var data = ChunkManager.getInstance().getPersistentDataChunk(location);
        if (data.isManagedBlock(location)) {
            return data.getInstanceAt(location);
        }
        if (CustomArcheology.plugin.getBlockMode() != BlockMode.CRAFTENGINE) {
            return null;
        }
        var state = CraftEngineBlocks.getCustomBlockState(location.getBlock());
        if (state == null) {
            return null;
        }
        var behavior = state.behavior().getFirst(BrushableBlockBehavior.class);
        if (behavior == null) {
            return null;
        }
        ArcheologyBlock block = BlockManager.getInstance().getBlock(behavior.archeologyId());
        if (block == null || !validDefinition(state.owner().value().id().toString(), block)) {
            return null;
        }
        return data.registerCraftEngineBlock(
                block, location, state.owner().value().id().toString(), false);
    }

    public static ItemStack createItem(ArcheologyBlock block, int amount) {
        if (!validDefinition(block.getCraftEngineBlockId(), block)) {
            throw new IllegalStateException(
                    "CE block is not ready or invalid: " + block.getCraftEngineBlockId());
        }
        var definition = CraftEngineItems.byId(block.getCraftEngineItemId());
        if (definition == null) {
            throw new IllegalArgumentException("Unknown CE item: " + block.getCraftEngineItemId());
        }
        ItemStack item = definition.buildBukkitItem();
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta != null) {
            itemMeta.getPersistentDataContainer().set(
                    NamespacedKeys.IS_ARCHEOLOGY_ITEM.getNamespacedKey(),
                    PersistentDataType.BOOLEAN,
                    true);
            itemMeta.getPersistentDataContainer().set(
                    NamespacedKeys.ARCHEOLOGY_BLOCK_ID.getNamespacedKey(),
                    PersistentDataType.STRING,
                    block.getName());
            item.setItemMeta(itemMeta);
        }
        item.setAmount(amount);
        return item;
    }

    public static boolean validReplaceDefinition(ArcheologyBlock block) {
        String id = block.getCraftEngineReplaceBlock();
        return id == null || ready && CraftEngineBlocks.byId(Key.of(id)) != null;
    }

    public static boolean matchesBlock(Block block, String id) {
        if (!ready) {
            return false;
        }
        var state = CraftEngineBlocks.getCustomBlockState(block);
        return state != null && state.owner().value().id().toString().equals(id);
    }

    public static Material getBukkitMaterial(String id) {
        if (ready) {
            var definition = CraftEngineBlocks.byId(Key.of(id));
            if (definition != null) {
                return CraftEngineBlocks.getBukkitBlockData(definition.defaultState()).getMaterial();
            }
        }
        return Material.STONE;
    }

    public static String serializeReplaceState(Location location, ArcheologyBlock block) {
        String id = block.getCraftEngineReplaceBlock();
        if (id == null) {
            return "";
        }
        var state = CraftEngineBlocks.getCustomBlockState(location.getBlock());
        if (state == null || !state.owner().value().id().toString().equals(id)) {
            return "";
        }
        return state.toString();
    }

    public static void restoreReplaceBlock(Location location, ArcheologyBlock block,
                                           String serializedState) {
        String id = block.getCraftEngineReplaceBlock();
        if (id == null) {
            CraftEngineBlocks.remove(location.getBlock(), false);
            location.getBlock().setType(block.getType());
            return;
        }
        var definition = CraftEngineBlocks.byId(Key.of(id));
        if (definition == null) {
            throw new IllegalStateException("Unknown CE replace-block: " + id);
        }
        var state = definition.defaultState();
        if (serializedState != null && !serializedState.isEmpty()) {
            try {
                var parsed = BlockStateParser.deserialize(serializedState);
                if (parsed != null && parsed.owner().value().id().toString().equals(id)) {
                    state = parsed;
                }
            } catch (RuntimeException ignored) {
            }
        }
        if (!CraftEngineBlocks.place(location, state, false)) {
            throw new IllegalStateException("Could not restore CE replace-block: " + id);
        }
    }

    public static ItemStack createToolItem(String id, int amount) {
        if (!ready) {
            throw new IllegalStateException("CraftEngine resources are not ready");
        }
        var definition = CraftEngineItems.byId(id);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown CE tool item: " + id);
        }
        ItemStack item = definition.buildBukkitItem();
        item.setAmount(amount);
        return item;
    }
}
