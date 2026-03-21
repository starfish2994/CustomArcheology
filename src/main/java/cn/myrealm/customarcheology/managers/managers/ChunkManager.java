package cn.myrealm.customarcheology.managers.managers;


import cn.myrealm.customarcheology.managers.BaseManager;
import cn.myrealm.customarcheology.mechanics.cores.ArcheologyBlock;
import cn.myrealm.customarcheology.mechanics.ArcheologyChunkSpawner;
import cn.myrealm.customarcheology.mechanics.cores.FakeTileBlock;
import cn.myrealm.customarcheology.mechanics.cores.PersistentDataChunk;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author rzt1020
 */
public class ChunkManager extends BaseManager {
    private static final Pattern REGION_FILE_PATTERN = Pattern.compile("r\\.(-?\\d+)\\.(-?\\d+)\\.mca");
    private static final int REMOVE_WORLD_BATCH_SIZE = 8;
    private static ChunkManager instance;
    private Map<Chunk, PersistentDataChunk> loadedChunks;
    private final Map<UUID, RemoveWorldTask> removingWorldTasks = new ConcurrentHashMap<>();
    private BukkitRunnable loadUnloadTask;
    public ChunkManager(JavaPlugin plugin) {
        super(plugin);
        instance = this;
    }

    @Override
    protected void onInit() {
        loadedChunks = new HashMap<>(5);
        loadUnloadTask = new BukkitRunnable() {
            @Override
            public void run() {
                Set<Chunk> newChunks = Bukkit.getOnlinePlayers().stream()
                        .flatMap(player -> {
                            Location loc = player.getLocation();
                            return Stream.of(
                                    loc.clone().add(8, 0, 8).getChunk(),
                                    loc.clone().add(8, 0, -8).getChunk(),
                                    loc.clone().add(-8, 0, 8).getChunk(),
                                    loc.clone().add(-8, 0, -8).getChunk()
                            );
                        })
                        .collect(Collectors.toSet());

                Set<Chunk> toUnload = new HashSet<>(loadedChunks.keySet());
                toUnload.removeAll(newChunks);
                toUnload.forEach(instance::unloadChunk);

                newChunks.removeAll(loadedChunks.keySet());
                newChunks.forEach(instance::loadChunk);

                BlockManager.getInstance().updateBlocks();
            }
        };
        loadUnloadTask.runTaskTimer(plugin, 10, 20);
    }


    @Override
    protected void onDisable() {
        loadUnloadTask.cancel();
        for (RemoveWorldTask task : removingWorldTasks.values()) {
            task.cancel();
        }
        removingWorldTasks.clear();
        for (PersistentDataChunk dataChunk : loadedChunks.values()) {
            dataChunk.saveChunk();
        }
    }

    public void removeBlock(Location location) {
        Chunk chunk = location.getChunk();
        if (chunkUnloaded(chunk)) {
            loadChunk(chunk);
        }
        loadedChunks.get(chunk).removeBlock(location);
    }

    public boolean removeManagedBlock(Location location) {
        location = location.getBlock().getLocation();
        World world = location.getWorld();
        if (world == null) {
            return false;
        }
        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;
        if (!world.isChunkGenerated(chunkX, chunkZ)) {
            return false;
        }
        boolean alreadyLoaded = world.isChunkLoaded(chunkX, chunkZ);
        Chunk chunk = location.getChunk();
        boolean trackedBefore = !chunkUnloaded(chunk);
        if (chunkUnloaded(chunk)) {
            loadChunk(chunk, false);
        }
        PersistentDataChunk dataChunk = loadedChunks.get(chunk);
        if (!dataChunk.isManagedBlock(location)) {
            if (!trackedBefore) {
                if (alreadyLoaded) {
                    forgetChunk(chunk);
                } else {
                    releaseChunk(chunk);
                }
            }
            return false;
        }
        dataChunk.removeBlock(location);
        if (!alreadyLoaded && !trackedBefore) {
            releaseChunk(chunk);
        }
        return true;
    }

    public void unregisterBlock(Location location) {
        Chunk chunk = location.getChunk();
        if (chunkUnloaded(chunk)) {
            loadChunk(chunk);
        }
        loadedChunks.get(chunk).unregisterBlock(location);
    }

    public void startRespawnCooldown(Location location) {
        Chunk chunk = location.getChunk();
        if (chunkUnloaded(chunk)) {
            loadChunk(chunk);
        }
        loadedChunks.get(chunk).startRespawnCooldown(location);
    }

    public void registerNewBlock(Chunk chunk, ArcheologyBlock block, Location location) {
        if (chunkUnloaded(chunk)) {
            loadChunk(chunk);
        }
        PersistentDataChunk dataChunk = loadedChunks.get(chunk);
        dataChunk.registerNewBlock(block, location);
    }

    public void loadChunk(Chunk chunk) {
        loadChunk(chunk, true);
    }

    public void loadChunk(Chunk chunk, boolean spawnNewBlocks) {
        PersistentDataChunk dataChunk = new PersistentDataChunk(chunk);
        if (spawnNewBlocks) {
            new ArcheologyChunkSpawner(chunk, dataChunk);
        }
        loadedChunks.put(chunk, dataChunk);
    }

    public void unloadChunk(Chunk chunk) {
        if (chunkUnloaded(chunk)) {
            return;
        }
        PersistentDataChunk dataChunk = loadedChunks.remove(chunk);
        if (dataChunk != null) {
            dataChunk.saveChunk();
        }
    }

    public boolean chunkUnloaded(Chunk chunk) {
        return !loadedChunks.containsKey(chunk);
    }

    public RemoveWorldAction toggleRemoveManagedBlocks(World world, BiConsumer<Integer, Integer> onProgress, Consumer<Integer> onComplete) {
        RemoveWorldTask existingTask = removingWorldTasks.get(world.getUID());
        if (existingTask != null) {
            existingTask.cancel();
            removingWorldTasks.remove(world.getUID(), existingTask);
            return RemoveWorldAction.CANCELED;
        }

        RemoveWorldTask newTask = new RemoveWorldTask(world, onProgress, onComplete);
        RemoveWorldTask previousTask = removingWorldTasks.putIfAbsent(world.getUID(), newTask);
        if (previousTask != null) {
            previousTask.cancel();
            removingWorldTasks.remove(world.getUID(), previousTask);
            return RemoveWorldAction.CANCELED;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<int[]> chunkCoords;
            try {
                chunkCoords = collectGeneratedChunkCoords(world);
            } catch (Exception e) {
                removingWorldTasks.remove(world.getUID(), newTask);
                plugin.getLogger().warning("Failed to prepare removeblock task for world " + world.getName() + ": " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!newTask.isCancelled()) {
                        onComplete.accept(0);
                    }
                });
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (newTask.isCancelled()) {
                    removingWorldTasks.remove(world.getUID(), newTask);
                    return;
                }
                runRemoveManagedBlocksTask(newTask, chunkCoords);
            });
        });
        return RemoveWorldAction.STARTED;
    }

    private List<int[]> collectGeneratedChunkCoords(World world) {
        File regionFolder = new File(world.getWorldFolder(), "region");
        File[] regionFiles = regionFolder.listFiles((dir, name) -> name.endsWith(".mca"));
        if (regionFiles == null || regionFiles.length == 0) {
            return Collections.emptyList();
        }

        List<int[]> chunkCoords = new ArrayList<>();
        Set<Long> visitedChunks = new HashSet<>();
        for (File regionFile : regionFiles) {
            Matcher matcher = REGION_FILE_PATTERN.matcher(regionFile.getName());
            if (!matcher.matches()) {
                continue;
            }
            int regionX = Integer.parseInt(matcher.group(1));
            int regionZ = Integer.parseInt(matcher.group(2));
            for (int localChunkX = 0; localChunkX < 32; localChunkX++) {
                for (int localChunkZ = 0; localChunkZ < 32; localChunkZ++) {
                    int chunkX = (regionX << 5) + localChunkX;
                    int chunkZ = (regionZ << 5) + localChunkZ;
                    long key = (((long) chunkX) << 32) ^ (chunkZ & 0xffffffffL);
                    if (!visitedChunks.add(key)) {
                        continue;
                    }
                    chunkCoords.add(new int[]{chunkX, chunkZ});
                }
            }
        }
        return chunkCoords;
    }

    private void runRemoveManagedBlocksTask(RemoveWorldTask task, List<int[]> chunkCoords) {
        World world = task.getWorld();
        int totalChunks = chunkCoords.size();
        Iterator<int[]> iterator = chunkCoords.iterator();
        BukkitRunnable runnable = new BukkitRunnable() {
            private int removedBlocks = 0;
            private int processedChunks = 0;

            @Override
            public void run() {
                try {
                    if (task.isCancelled()) {
                        removingWorldTasks.remove(world.getUID(), task);
                        cancel();
                        return;
                    }
                    int processed = 0;
                    while (processed < REMOVE_WORLD_BATCH_SIZE && iterator.hasNext()) {
                        int[] chunkCoord = iterator.next();
                        int chunkX = chunkCoord[0];
                        int chunkZ = chunkCoord[1];
                        processed++;
                        processedChunks++;

                        if (!world.isChunkGenerated(chunkX, chunkZ)) {
                            continue;
                        }

                        boolean alreadyLoaded = world.isChunkLoaded(chunkX, chunkZ);
                        Chunk chunk = world.getChunkAt(chunkX, chunkZ);
                        boolean trackedBefore = !chunkUnloaded(chunk);
                        if (chunkUnloaded(chunk)) {
                            loadChunk(chunk, false);
                        }
                        PersistentDataChunk dataChunk = loadedChunks.get(chunk);
                        int removedInChunk = dataChunk.removeAllBlocks();
                        removedBlocks += removedInChunk;
                        if (!trackedBefore) {
                            if (alreadyLoaded && removedInChunk == 0) {
                                forgetChunk(chunk);
                            } else if (!alreadyLoaded) {
                                releaseChunk(chunk);
                            }
                        }
                    }

                    task.reportProgress(processedChunks, totalChunks);

                    if (!iterator.hasNext()) {
                        removingWorldTasks.remove(world.getUID(), task);
                        cancel();
                        task.onComplete(removedBlocks);
                    }
                } catch (Exception e) {
                    removingWorldTasks.remove(world.getUID(), task);
                    cancel();
                    plugin.getLogger().warning("Failed to remove managed blocks in world " + world.getName() + ": " + e.getMessage());
                    task.onComplete(0);
                }
            }
        };
        task.setRunnable(runnable);
        runnable.runTaskTimer(plugin, 1L, 1L);
    }

    public static ChunkManager getInstance() {
        return instance;
    }

    public boolean isArcheologyBlock(Location location) {
        PersistentDataChunk dataChunk = getPersistentDataChunk(location);
        return dataChunk.isArcheologyBlock(location);
    }

    public ArcheologyBlock getArcheologyBlock(Location location) {
        PersistentDataChunk dataChunk = getPersistentDataChunk(location);
        return dataChunk.getArcheologyBlock(location);
    }

    public boolean isManagedBlock(Location location) {
        PersistentDataChunk dataChunk = getPersistentDataChunk(location);
        return dataChunk.isManagedBlock(location);
    }

    public boolean isRespawningBlock(Location location) {
        PersistentDataChunk dataChunk = getPersistentDataChunk(location);
        return dataChunk.isRespawningBlock(location);
    }

    public PersistentDataChunk getPersistentDataChunk(Location location) {
        location = location.getBlock().getLocation();
        Chunk chunk = location.getChunk();
        if (chunkUnloaded(chunk)) {
            loadChunk(chunk);
        }
        return loadedChunks.get(chunk);
    }


    public List<FakeTileBlock> getFakeTileBlocks() {
        List<FakeTileBlock> fakeTileBlocks = new ArrayList<>();
        for (PersistentDataChunk dataChunk : loadedChunks.values()) {
            fakeTileBlocks.addAll(dataChunk.getFakeTileBlocks());
        }
        return fakeTileBlocks;
    }

    public FakeTileBlock getFakeTileBlock(Location location) {
        Chunk chunk = location.getChunk();
        if (chunkUnloaded(chunk)) {
            loadChunk(chunk);
        }
        return loadedChunks.get(chunk).getFakeTileBlock(location);
    }

    private void releaseChunk(Chunk chunk) {
        PersistentDataChunk dataChunk = loadedChunks.remove(chunk);
        if (dataChunk != null) {
            dataChunk.saveChunk();
        }
        chunk.unload(true);
    }

    private void forgetChunk(Chunk chunk) {
        loadedChunks.remove(chunk);
    }

}

class RemoveWorldTask {
    private final World world;
    private final BiConsumer<Integer, Integer> onProgress;
    private final Consumer<Integer> onComplete;
    private volatile boolean cancelled;
    private BukkitRunnable runnable;
    private int lastReportedBucket = 0;

    RemoveWorldTask(World world, BiConsumer<Integer, Integer> onProgress, Consumer<Integer> onComplete) {
        this.world = world;
        this.onProgress = onProgress;
        this.onComplete = onComplete;
    }

    public World getWorld() {
        return world;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setRunnable(BukkitRunnable runnable) {
        this.runnable = runnable;
    }

    public void reportProgress(int processedChunks, int totalChunks) {
        if (cancelled || totalChunks <= 0) {
            return;
        }
        int percent = Math.min(100, (processedChunks * 100) / totalChunks);
        int bucket = percent / 10;
        if (bucket > lastReportedBucket && percent < 100) {
            lastReportedBucket = bucket;
            onProgress.accept(processedChunks, totalChunks);
        }
    }

    public void onComplete(int removedBlocks) {
        if (!cancelled) {
            onComplete.accept(removedBlocks);
        }
    }

    public void cancel() {
        cancelled = true;
        if (runnable != null) {
            runnable.cancel();
        }
    }
}
