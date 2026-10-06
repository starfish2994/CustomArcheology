package cn.myrealm.customarcheology.mechanics.cores;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.myrealm.customarcheology.CustomArcheology;
import cn.myrealm.customarcheology.enums.NamespacedKeys;
import cn.myrealm.customarcheology.managers.managers.BlockManager;
import cn.myrealm.customarcheology.managers.managers.ChunkManager;
import cn.myrealm.customarcheology.managers.managers.PlayerManager;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

class ArcheologyInstanceTest {
    private final List<MockedStatic<?>> statics = new ArrayList<>();
    private final PriorityQueue<Scheduled> tasks =
            new PriorityQueue<>(Comparator.comparingLong(task -> task.at));
    private ArcheologyBlock definition;
    private ChunkManager chunks;
    private PersistentDataChunk data;
    private World world;
    private Location location;
    private ItemStack reward;
    private ItemStack tool;
    private Damageable durability;
    private long now;

    @BeforeEach
    void setUp() {
        CustomArcheology plugin = mock(CustomArcheology.class);
        CustomArcheology.plugin = plugin;
        CustomArcheology.yearVersion = 1;
        CustomArcheology.majorVersion = 21;
        CustomArcheology.minorVersion = 11;
        when(plugin.getConfig()).thenReturn(new YamlConfiguration());
        when(plugin.getName()).thenReturn("CustomArcheology");
        when(plugin.namespace()).thenReturn("customarcheology");
        Server server = mock(Server.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getScheduler()).thenReturn(scheduler);
        when(scheduler.runTaskLater(any(Plugin.class), any(Runnable.class), anyLong()))
                .thenAnswer(
                        invocation -> {
                            Scheduled scheduled =
                                    new Scheduled(
                                            now + invocation.getArgument(2, Long.class),
                                            invocation.getArgument(1));
                            tasks.add(scheduled);
                            return scheduled.task;
                        });
        when(scheduler.runTaskTimer(any(Plugin.class), any(Runnable.class), anyLong(), anyLong()))
                .thenReturn(mock(BukkitTask.class));
        world = mock(World.class);
        location = new Location(world, 10, 64, 10);
        Block worldBlock = mock(Block.class);
        when(world.getBlockAt(10, 64, 10)).thenReturn(worldBlock);
        when(world.getBlockAt(any(Location.class))).thenReturn(worldBlock);
        when(worldBlock.getLocation()).thenReturn(location);
        when(world.getNearbyEntities(any(Location.class), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of());
        MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
        statics.add(bukkit);
        BlockData particleData = mock(BlockData.class);
        bukkit.when(() -> Bukkit.createBlockData(any(Material.class))).thenReturn(particleData);
        bukkit.when(() -> Bukkit.createBlockData(any(Material.class), nullable(String.class)))
                .thenReturn(particleData);

        definition = mock(ArcheologyBlock.class);
        when(definition.isValid()).thenReturn(true);
        when(definition.getType()).thenReturn(Material.STONE);
        State stage = mock(State.class);
        when(stage.getHardness()).thenReturn(1.0);
        when(definition.getDefaultState()).thenReturn(stage);
        when(definition.getStates()).thenReturn(List.of(stage, stage, stage));
        when(definition.getEfficiency(any())).thenReturn(1.0);
        when(definition.getConsumeDurability()).thenReturn(1);
        reward = mock(ItemStack.class);
        when(definition.roll(any())).thenReturn(reward);
        tool = mock(ItemStack.class);
        durability = mock(Damageable.class);
        when(tool.getItemMeta()).thenReturn(durability);
        Material toolType = mock(Material.class);
        when(toolType.getMaxDurability()).thenReturn((short) 64);
        when(tool.getType()).thenReturn(toolType);

        BlockManager blocks = mock(BlockManager.class);
        when(blocks.getBlock("suspicious_stone")).thenReturn(definition);
        MockedStatic<BlockManager> blockStatic = mockStatic(BlockManager.class);
        statics.add(blockStatic);
        blockStatic.when(BlockManager::getInstance).thenReturn(blocks);
        chunks = mock(ChunkManager.class);
        data = mock(PersistentDataChunk.class);
        when(chunks.getPersistentDataChunk(location)).thenReturn(data);
        MockedStatic<ChunkManager> chunkStatic = mockStatic(ChunkManager.class);
        statics.add(chunkStatic);
        chunkStatic.when(ChunkManager::getInstance).thenReturn(chunks);
        MockedStatic<PlayerManager> playerStatic = mockStatic(PlayerManager.class);
        statics.add(playerStatic);
        playerStatic.when(PlayerManager::getInstance).thenReturn(mock(PlayerManager.class));
    }

    @AfterEach
    void tearDown() {
        for (int i = statics.size() - 1; i >= 0; i--) {
            statics.get(i).close();
        }
        CustomArcheology.plugin = null;
    }

    @Test
    void completesOnceAndPersistsRewardBeforeProgress() {
        TestBlock instance = new TestBlock(null, null);
        instance.play(BlockFace.UP, tool);
        verify(data).saveData();
        instance.play(BlockFace.UP, tool);
        advance(80);
        advance(200);
        assertEquals(List.of(1, 2, 3, -1), instance.rendered);
        verify(definition, times(1)).roll(tool);
        verify(world, times(1)).dropItem(any(Location.class), same(reward));
        verify(durability).setDamage(1);
        verify(chunks).unregisterBlock(location);
    }

    @Test
    void pauseCancelsCompletionAndResumingKeepsReward() {
        TestBlock instance = new TestBlock(null, null);
        instance.play(BlockFace.UP, tool);
        advance(20);
        instance.pause();
        advance(60);
        assertEquals(List.of(1, 0), instance.rendered);
        verify(world, never()).dropItem(any(), any());
        instance.play(BlockFace.UP, tool);
        advance(80);
        verify(definition, times(1)).roll(tool);
        verify(world, times(1)).dropItem(any(), same(reward));
    }

    @Test
    void unloadCancelsScheduledWorkAndRetainsReward() {
        TestBlock instance = new TestBlock(null, null);
        instance.play(BlockFace.UP, tool);
        advance(20);
        instance.prepareForChunkUnload();
        advance(200);
        assertSame(reward, instance.getReward());
        assertFalse(instance.isActive());
        verify(world, never()).dropItem(any(), any());
        TestBlock restored = new TestBlock(instance.getReward(), null);
        restored.play(BlockFace.UP, tool);
        advance(80);
        verify(definition, times(1)).roll(tool);
        verify(world).dropItem(any(), same(reward));
    }

    @Test
    void replacementCancelsWorkWithoutRewardOrWorldOverwrite() {
        TestBlock instance = new TestBlock(null, null);
        instance.play(BlockFace.UP, tool);
        instance.owned = false;
        advance(100);
        assertTrue(instance.rendered.isEmpty());
        verify(chunks).unregisterBlock(location);
        verify(world, never()).dropItem(any(), any());
    }

    @Test
    void expiredCooldownCannotOverwriteReplacement() {
        TestBlock instance = new TestBlock(null, 1L);
        instance.owned = false;
        instance.placeBlock();
        assertTrue(instance.rendered.isEmpty());
        verify(chunks).unregisterBlock(location);
    }

    @Test
    void completedBlockEntersCooldownAndDoesNotDropTwice() {
        when(definition.shouldRespawn()).thenReturn(true);
        when(definition.getRespawnDelayMillis()).thenReturn(60_000L);
        TestBlock instance = new TestBlock(null, null);
        instance.play(BlockFace.UP, tool);
        advance(80);
        assertTrue(instance.isCoolingDown());
        assertNull(instance.getReward());
        instance.play(BlockFace.UP, tool);
        advance(200);
        verify(world, times(1)).dropItem(any(), same(reward));
        verify(data, times(2)).saveData();
    }

    @Test
    void oldRecordDefaultsToLegacyAndRemovalClearsRuntimeIndex() {
        String name = "suspicious_stone_old";
        Map<NamespacedKey, Object> values = new HashMap<>();
        values.put(NamespacedKeys.ARCHEOLOGY_ARRAY.getNamespacedKey(), new String[] {name});
        values.put(NamespacedKeys.ARCHEOLOGY_BLOCK_LOC.getNamespacedKey(name), location);
        when(world.getBlockAt(location).getType()).thenReturn(Material.STONE);
        PersistentDataChunk persistent = new PersistentDataChunk(storedChunk(values));
        assertEquals(BlockMode.LEGACY, persistent.getInstanceAt(location).getMode());
        persistent.saveData();
        assertEquals(
                "legacy", values.get(NamespacedKeys.ARCHEOLOGY_BLOCK_MODE.getNamespacedKey(name)));
        persistent.removeBlock(location);
        assertFalse(persistent.isManagedBlock(location));
        assertNull(persistent.getInstanceAt(location));
        assertArrayEquals(
                new String[0],
                (String[]) values.get(NamespacedKeys.ARCHEOLOGY_ARRAY.getNamespacedKey()));
    }

    @Test
    void unavailableCraftEngineRecordIsPreservedAndReserved() {
        String name = "suspicious_stone_ce";
        Map<NamespacedKey, Object> values = new HashMap<>();
        values.put(NamespacedKeys.ARCHEOLOGY_ARRAY.getNamespacedKey(), new String[] {name});
        values.put(NamespacedKeys.ARCHEOLOGY_BLOCK_LOC.getNamespacedKey(name), location);
        values.put(NamespacedKeys.ARCHEOLOGY_BLOCK_MODE.getNamespacedKey(name), "craftengine");
        values.put(NamespacedKeys.ARCHEOLOGY_BLOCK_ITEM.getNamespacedKey(name), reward);
        values.put(NamespacedKeys.ARCHEOLOGY_BLOCK_ORIGINAL.getNamespacedKey(name),
                "mypack:ancient_stone[age=2]");
        when(CustomArcheology.plugin.isCraftEngineAvailable()).thenReturn(false);
        PersistentDataChunk persistent = new PersistentDataChunk(storedChunk(values));
        assertTrue(persistent.isManagedBlock(location));
        assertNull(persistent.getInstanceAt(location));
        persistent.saveChunk();
        assertSame(reward, values.get(NamespacedKeys.ARCHEOLOGY_BLOCK_ITEM.getNamespacedKey(name)));
        assertEquals(
                "craftengine",
                values.get(NamespacedKeys.ARCHEOLOGY_BLOCK_MODE.getNamespacedKey(name)));
        assertEquals("mypack:ancient_stone[age=2]",
                values.get(NamespacedKeys.ARCHEOLOGY_BLOCK_ORIGINAL.getNamespacedKey(name)));
        verify(world.getBlockAt(location), never()).setType(any());
    }

    private Chunk storedChunk(Map<NamespacedKey, Object> values) {
        PersistentDataContainer container =
                mock(
                        PersistentDataContainer.class,
                        invocation -> {
                            return switch (invocation.getMethod().getName()) {
                                case "has" -> values.containsKey(invocation.getArgument(0));
                                case "get" -> values.get(invocation.getArgument(0));
                                case "getOrDefault" ->
                                        values.getOrDefault(
                                                invocation.getArgument(0),
                                                invocation.getArgument(2));
                                case "set" -> {
                                    values.put(
                                            invocation.getArgument(0), invocation.getArgument(2));
                                    yield null;
                                }
                                case "remove" -> {
                                    values.remove(invocation.getArgument(0));
                                    yield null;
                                }
                                default -> null;
                            };
                        });
        Chunk chunk = mock(Chunk.class);
        when(chunk.getPersistentDataContainer()).thenReturn(container);
        return chunk;
    }

    private void advance(long ticks) {
        long until = now + ticks;
        while (!tasks.isEmpty() && tasks.peek().at <= until) {
            Scheduled scheduled = tasks.remove();
            now = scheduled.at;
            if (!scheduled.cancelled) {
                scheduled.runnable.run();
            }
        }
        now = until;
    }

    private static class Scheduled {
        final long at;
        final Runnable runnable;
        final BukkitTask task = mock(BukkitTask.class);
        boolean cancelled;

        Scheduled(long at, Runnable runnable) {
            this.at = at;
            this.runnable = runnable;
            doAnswer(
                            invocation -> {
                                cancelled = true;
                                return null;
                            })
                    .when(task)
                    .cancel();
        }
    }

    private class TestBlock extends ArcheologyInstance {
        final List<Integer> rendered = new ArrayList<>();
        boolean owned = true;

        TestBlock(ItemStack reward, Long respawnAt) {
            super("suspicious_stone_test", ArcheologyInstanceTest.this.location, reward, respawnAt);
        }

        @Override
        public BlockMode getMode() {
            return BlockMode.LEGACY;
        }

        @Override
        protected void renderStage(int stage) {
            rendered.add(stage);
        }

        @Override
        protected void renderFinished() {
            rendered.add(-1);
        }

        @Override
        protected void restoreOriginal() {
            rendered.add(-2);
        }

        @Override
        protected void unloadAppearance() {}

        @Override
        protected boolean matchesWorld() {
            return owned;
        }
    }
}
