package cn.myrealm.customarcheology.mechanics.cores;


import cn.myrealm.customarcheology.CustomArcheology;
import cn.myrealm.customarcheology.hooks.craftengine.CraftEngineBlockInstance;
import cn.myrealm.customarcheology.hooks.craftengine.CraftEngineSupport;
import cn.myrealm.customarcheology.enums.Config;
import cn.myrealm.customarcheology.enums.NamespacedKeys;
import cn.myrealm.customarcheology.mechanics.persistent_data.ItemStackTagType;
import cn.myrealm.customarcheology.mechanics.persistent_data.LocationTagType;
import cn.myrealm.customarcheology.mechanics.persistent_data.StringArrayTagType;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @author rzt1020
 */
public class PersistentDataChunk {
    private final Chunk chunk;
    private List<String> blockNameList;
    private static final StringArrayTagType STRING_ARRAY_TYPE = new StringArrayTagType(StandardCharsets.UTF_8);
    private static final ItemStackTagType ITEM_STACK_TYPE = new ItemStackTagType();
    private static final LocationTagType LOCATION_TYPE = new LocationTagType();
    private final Map<Location, ArcheologyInstance> loadedLocationBlocks = new HashMap<>();
    private final Set<Location> reservedLocations = new HashSet<>();
    public PersistentDataChunk(Chunk chunk) {
        this.chunk = chunk;
        loadChunk();
    }

    public void loadChunk() {
        loadBlockNames();
        loadBlockLocations();
    }
    private void loadBlockNames() {
        if (chunk.getPersistentDataContainer().has(NamespacedKeys.ARCHEOLOGY_ARRAY.getNamespacedKey(), STRING_ARRAY_TYPE)) {
            blockNameList = new ArrayList<>(Arrays.asList(Objects.requireNonNull(chunk.getPersistentDataContainer().get(NamespacedKeys.ARCHEOLOGY_ARRAY.getNamespacedKey(), STRING_ARRAY_TYPE))));
        } else {
            blockNameList = new ArrayList<>();
        }
    }
    private boolean deferredRecords;

    private void loadBlockLocations() {
        deferredRecords = false;
        for (String blockName : blockNameList) {
            Location location = null;
            if (chunk.getPersistentDataContainer().has(NamespacedKeys.ARCHEOLOGY_BLOCK_LOC.getNamespacedKey(blockName), LOCATION_TYPE)) {
                location = chunk.getPersistentDataContainer().get(NamespacedKeys.ARCHEOLOGY_BLOCK_LOC.getNamespacedKey(blockName), LOCATION_TYPE);
            }
            ItemStack reward = null;
            if (chunk.getPersistentDataContainer().has(NamespacedKeys.ARCHEOLOGY_BLOCK_ITEM.getNamespacedKey(blockName), ITEM_STACK_TYPE)) {
                reward = chunk.getPersistentDataContainer().get(NamespacedKeys.ARCHEOLOGY_BLOCK_ITEM.getNamespacedKey(blockName), ITEM_STACK_TYPE);
            }
            Long respawnAt = null;
            if (chunk.getPersistentDataContainer().has(NamespacedKeys.ARCHEOLOGY_BLOCK_RESPAWN.getNamespacedKey(blockName), PersistentDataType.LONG)) {
                respawnAt = chunk.getPersistentDataContainer().get(NamespacedKeys.ARCHEOLOGY_BLOCK_RESPAWN.getNamespacedKey(blockName), PersistentDataType.LONG);
            }
            if (Objects.nonNull(location)) {
                location = location.getBlock().getLocation();
                reservedLocations.add(location);
                if (loadedLocationBlocks.containsKey(location)) {
                    continue;
                }
                String modeName = chunk.getPersistentDataContainer().getOrDefault(
                        NamespacedKeys.ARCHEOLOGY_BLOCK_MODE.getNamespacedKey(blockName),
                        PersistentDataType.STRING,
                        BlockMode.LEGACY.configValue());
                BlockMode mode;
                try {
                    mode = BlockMode.parse(modeName);
                } catch (IllegalArgumentException exception) {
                    CustomArcheology.plugin.getLogger().warning(exception.getMessage());
                    continue;
                }
                ArcheologyInstance archeologyInstance;
                if (mode == BlockMode.CRAFTENGINE) {
                    if (!CustomArcheology.plugin.isCraftEngineAvailable() || !CraftEngineSupport.isReady()) {
                        deferredRecords = true;
                        continue;
                    }
                    String backend = chunk.getPersistentDataContainer().get(
                            NamespacedKeys.ARCHEOLOGY_BLOCK_BACKEND.getNamespacedKey(blockName),
                            PersistentDataType.STRING);
                    String expected = chunk.getPersistentDataContainer().get(
                            NamespacedKeys.ARCHEOLOGY_BLOCK_EXPECTED.getNamespacedKey(blockName),
                            PersistentDataType.STRING);
                    String original = chunk.getPersistentDataContainer().get(
                            NamespacedKeys.ARCHEOLOGY_BLOCK_ORIGINAL.getNamespacedKey(blockName),
                            PersistentDataType.STRING);
                    if (backend == null || backend.isEmpty()) {
                        CustomArcheology.plugin.getLogger().warning("Missing CE backend for " + blockName);
                        continue;
                    }
                    archeologyInstance = new CraftEngineBlockInstance(
                            blockName, location, reward, respawnAt, backend, expected, original);
                } else {
                    archeologyInstance = new FakeTileBlock(blockName, location, reward, respawnAt);
                }
                if (archeologyInstance.isValid()) {
                    loadedLocationBlocks.put(location, archeologyInstance);
                }
            }
        }
    }

    public void saveChunk() {
        if (Objects.isNull(blockNameList)) {
            return;
        }
        for (ArcheologyInstance archeologyInstance : loadedLocationBlocks.values()) {
            archeologyInstance.prepareForChunkUnload();
        }
        saveData();
    }

    public void saveData() {
        saveBlockNames();
        saveBlockLocations();
        saveBlockRewards();
        saveBlockRespawns();
        for (ArcheologyInstance block : loadedLocationBlocks.values()) {
            chunk.getPersistentDataContainer().set(NamespacedKeys.ARCHEOLOGY_BLOCK_MODE.getNamespacedKey(block.getBlockName()), PersistentDataType.STRING, block.getMode().configValue());
            chunk.getPersistentDataContainer().set(NamespacedKeys.ARCHEOLOGY_BLOCK_BACKEND.getNamespacedKey(block.getBlockName()), PersistentDataType.STRING, block.getBackendId());
            chunk.getPersistentDataContainer().set(NamespacedKeys.ARCHEOLOGY_BLOCK_EXPECTED.getNamespacedKey(block.getBlockName()), PersistentDataType.STRING, block.getExpectedState());
            chunk.getPersistentDataContainer().set(NamespacedKeys.ARCHEOLOGY_BLOCK_ORIGINAL.getNamespacedKey(block.getBlockName()), PersistentDataType.STRING, block.getOriginalState());
        }
    }

    private void saveBlockRewards() {
        for (ArcheologyInstance archeologyInstance : loadedLocationBlocks.values()) {
            if (Objects.nonNull(archeologyInstance.getReward())) {
                chunk.getPersistentDataContainer().set(NamespacedKeys.ARCHEOLOGY_BLOCK_ITEM.getNamespacedKey(archeologyInstance.getBlockName()), ITEM_STACK_TYPE, archeologyInstance.getReward() );
            } else if (chunk.getPersistentDataContainer().has(NamespacedKeys.ARCHEOLOGY_BLOCK_ITEM.getNamespacedKey(archeologyInstance.getBlockName()), ITEM_STACK_TYPE)) {
                chunk.getPersistentDataContainer().remove(NamespacedKeys.ARCHEOLOGY_BLOCK_ITEM.getNamespacedKey(archeologyInstance.getBlockName()));
            }
        }
    }

    private void saveBlockRespawns() {
        for (ArcheologyInstance archeologyInstance : loadedLocationBlocks.values()) {
            Long respawnAt = archeologyInstance.getRespawnAt();
            if (Objects.nonNull(respawnAt)) {
                chunk.getPersistentDataContainer().set(NamespacedKeys.ARCHEOLOGY_BLOCK_RESPAWN.getNamespacedKey(archeologyInstance.getBlockName()), PersistentDataType.LONG, respawnAt);
            } else if (chunk.getPersistentDataContainer().has(NamespacedKeys.ARCHEOLOGY_BLOCK_RESPAWN.getNamespacedKey(archeologyInstance.getBlockName()), PersistentDataType.LONG)) {
                chunk.getPersistentDataContainer().remove(NamespacedKeys.ARCHEOLOGY_BLOCK_RESPAWN.getNamespacedKey(archeologyInstance.getBlockName()));
            }
        }
    }

    private void saveBlockNames() {
        String [] array = new String[blockNameList.size()];
        blockNameList.toArray(array);
        chunk.getPersistentDataContainer().set(NamespacedKeys.ARCHEOLOGY_ARRAY.getNamespacedKey(), STRING_ARRAY_TYPE, array);
    }

    private void saveBlockLocations() {
        for (Location location : loadedLocationBlocks.keySet()) {
            ArcheologyInstance block = loadedLocationBlocks.get(location);
            chunk.getPersistentDataContainer().set(NamespacedKeys.ARCHEOLOGY_BLOCK_LOC.getNamespacedKey(block.getBlockName()), LOCATION_TYPE, location);
        }
    }

    public void removeBlock(Location location) {
        location = location.getBlock().getLocation();
        String removedBlockData = getBlockName(location);
        if (removedBlockData == null) {
            return;
        }
        ArcheologyInstance archeologyInstance = loadedLocationBlocks.remove(location);
        if (archeologyInstance != null) {
            archeologyInstance.removeBlock();
        }
        unregisterBlockData(location, removedBlockData);
    }

    public int removeAllBlocks() {
        List<Location> locations = new ArrayList<>(reservedLocations);
        for (Location location : locations) {
            removeBlock(location);
        }
        return locations.size();
    }

    public void unregisterBlock(Location location) {
        location = location.getBlock().getLocation();
        String removedBlockData = getBlockName(location);
        if (removedBlockData == null) {
            return;
        }
        ArcheologyInstance archeologyInstance = loadedLocationBlocks.remove(location);
        if (archeologyInstance != null) {
            archeologyInstance.unregisterBlock();
        }
        unregisterBlockData(location, removedBlockData);
    }

    private void unregisterBlockData(Location location, String removedBlockData) {
        reservedLocations.remove(location);
        blockNameList.remove(removedBlockData);
        chunk.getPersistentDataContainer().remove(NamespacedKeys.ARCHEOLOGY_BLOCK_MODE.getNamespacedKey(removedBlockData));
        chunk.getPersistentDataContainer().remove(NamespacedKeys.ARCHEOLOGY_BLOCK_BACKEND.getNamespacedKey(removedBlockData));
        chunk.getPersistentDataContainer().remove(NamespacedKeys.ARCHEOLOGY_BLOCK_EXPECTED.getNamespacedKey(removedBlockData));
        chunk.getPersistentDataContainer().remove(NamespacedKeys.ARCHEOLOGY_BLOCK_ORIGINAL.getNamespacedKey(removedBlockData));
        saveBlockNames();
        if (chunk.getPersistentDataContainer().has(NamespacedKeys.ARCHEOLOGY_BLOCK_LOC.getNamespacedKey(removedBlockData), LOCATION_TYPE)) {
            chunk.getPersistentDataContainer().remove(NamespacedKeys.ARCHEOLOGY_BLOCK_LOC.getNamespacedKey(removedBlockData));
        }
        if (chunk.getPersistentDataContainer().has(NamespacedKeys.ARCHEOLOGY_BLOCK_ITEM.getNamespacedKey(removedBlockData), ITEM_STACK_TYPE)) {
            chunk.getPersistentDataContainer().remove(NamespacedKeys.ARCHEOLOGY_BLOCK_ITEM.getNamespacedKey(removedBlockData));
        }
        if (chunk.getPersistentDataContainer().has(NamespacedKeys.ARCHEOLOGY_BLOCK_RESPAWN.getNamespacedKey(removedBlockData), PersistentDataType.LONG)) {
            chunk.getPersistentDataContainer().remove(NamespacedKeys.ARCHEOLOGY_BLOCK_RESPAWN.getNamespacedKey(removedBlockData));
        }
    }

    public void registerNewBlock(ArcheologyBlock block, Location location) {
        if (CustomArcheology.plugin.getBlockMode() == BlockMode.CRAFTENGINE) {
            registerCraftEngineBlock(block, location, block.getCraftEngineBlockId(), true);
            return;
        }
        location = location.getBlock().getLocation();
        if (isManagedBlock(location)) {
            throw new IllegalStateException("An archeology block already exists at " + location);
        }
        String name = generateBlockName(block);
        ArcheologyInstance instance = new FakeTileBlock(name, location, null, null);
        instance.placeNewBlock();
        blockNameList.add(name);
        reservedLocations.add(location);
        loadedLocationBlocks.put(location, instance);
        saveData();
    }

    public ArcheologyInstance registerCraftEngineBlock(ArcheologyBlock block, Location location, String backend, boolean place) {
        location = location.getBlock().getLocation();
        if (isManagedBlock(location)) {
            return loadedLocationBlocks.get(location);
        }
        String name = generateBlockName(block);
        String original = place ? CraftEngineSupport.serializeReplaceState(location, block) : "";
        ArcheologyInstance instance = new CraftEngineBlockInstance(
                name, location, null, null, backend, null, original);
        if (!instance.isValid()) {
            throw new IllegalArgumentException("Invalid CE archeology definition: " + backend);
        }
        if (place) {
            instance.placeNewBlock();
        }
        blockNameList.add(name);
        reservedLocations.add(location);
        loadedLocationBlocks.put(location, instance);
        saveData();
        return instance;
    }

    public boolean isArcheologyBlock(Location location) {
        return loadedLocationBlocks.containsKey(location) && loadedLocationBlocks.get(location).isActive();
    }

    public ArcheologyBlock getArcheologyBlock(Location location) {
        if (loadedLocationBlocks.containsKey(location) && loadedLocationBlocks.get(location).isActive()) {
            return loadedLocationBlocks.get(location).getArcheologyBlock();
        }
        return null;
    }

    public boolean isManagedBlock(Location location) {
        return reservedLocations.contains(location.getBlock().getLocation());
    }

    public boolean isRespawningBlock(Location location) {
        return loadedLocationBlocks.containsKey(location) && loadedLocationBlocks.get(location).isCoolingDown();
    }


    public Collection<ArcheologyInstance> getInstances() {
        if (deferredRecords && CustomArcheology.plugin.isCraftEngineAvailable() && CraftEngineSupport.isReady()) {
            loadBlockLocations();
        }
        return new ArrayList<>(loadedLocationBlocks.values());
    }

    public ArcheologyInstance getInstanceAt(Location location) {
        if (loadedLocationBlocks.containsKey(location)){
            return loadedLocationBlocks.get(location);
        }
        return null;
    }

    public void startRespawnCooldown(Location location) {
        location = location.getBlock().getLocation();
        if (!loadedLocationBlocks.containsKey(location)) {
            return;
        }
        loadedLocationBlocks.get(location).startRespawnCooldown();
        saveBlockRewards();
        saveBlockRespawns();
    }

    private String getBlockName(Location location) {
        ArcheologyInstance loaded = loadedLocationBlocks.get(location);
        if (loaded != null) {
            return loaded.getBlockName();
        }
        for (String blockName : blockNameList) {
            Location stored = chunk.getPersistentDataContainer().get(
                    NamespacedKeys.ARCHEOLOGY_BLOCK_LOC.getNamespacedKey(blockName), LOCATION_TYPE);
            if (location.equals(stored)) {
                return blockName;
            }
        }
        return null;
    }

    private String generateBlockName(ArcheologyBlock block) {
        if (Config.BLOCK_SAVE.asString().equals("UUID")) {
            return block.getName() + "_" + UUID.randomUUID();
        }
        return block.getName() + "_" + CustomArcheology.RANDOM.nextInt();
    }

}
