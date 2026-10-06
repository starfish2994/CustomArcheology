package cn.myrealm.customarcheology.mechanics.cores;

import cn.myrealm.customarcheology.CustomArcheology;
import cn.myrealm.customarcheology.enums.Config;
import cn.myrealm.customarcheology.enums.NamespacedKeys;
import cn.myrealm.customarcheology.managers.managers.BlockManager;
import cn.myrealm.customarcheology.managers.managers.ChunkManager;
import cn.myrealm.customarcheology.managers.managers.PlayerManager;
import cn.myrealm.customarcheology.mechanics.persistent_data.ItemStackTagType;
import cn.myrealm.customarcheology.utils.CommonUtil;
import cn.myrealm.customarcheology.utils.PacketUtil;

import com.github.retrooper.packetevents.util.Quaternion4f;
import com.github.retrooper.packetevents.util.Vector3f;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Shared brushing lifecycle; subclasses own the world block and its appearance. */
public abstract class ArcheologyInstance {
    private static final AtomicInteger IDS = new AtomicInteger(1_500_000_000);
    protected final int entityId = IDS.getAndAdd(2);
    protected final String blockName;
    protected final ArcheologyBlock block;
    protected final Location location;
    protected final Set<Player> sentPlayers = new HashSet<>();
    private final Set<Player> rewardViewers = new HashSet<>();
    private ItemStack reward, tool;
    private Long respawnAt;
    private BlockFace face;
    private int stage;
    private boolean brushing, closed;
    private BukkitTask progressTask, particleTask;

    protected ArcheologyInstance(String name, Location location, ItemStack reward, Long respawnAt) {
        this.blockName = name;
        this.block = BlockManager.getInstance().getBlock(name.substring(0, name.lastIndexOf('_')));
        this.location = location.getBlock().getLocation();
        this.reward = reward;
        this.respawnAt = respawnAt;
    }

    public abstract BlockMode getMode();

    protected abstract void renderStage(int stage);

    protected abstract void renderFinished();

    protected abstract void restoreOriginal();

    protected abstract void unloadAppearance();

    protected abstract boolean matchesWorld();

    public String getBackendId() {
        return "";
    }

    public String getExpectedState() {
        return "";
    }

    public String getOriginalState() {
        return "";
    }

    public String getBlockName() {
        return blockName;
    }

    public Location getLocation() {
        return location.clone();
    }

    public ArcheologyBlock getArcheologyBlock() {
        return block;
    }

    public ItemStack getReward() {
        return reward;
    }

    public Long getRespawnAt() {
        return respawnAt;
    }

    public boolean isValid() {
        return block != null && block.isValid();
    }

    public boolean isCoolingDown() {
        return respawnAt != null;
    }

    public boolean isActive() {
        return !closed && respawnAt == null && matchesWorld();
    }

    protected State stageState(int index) {
        return index == 0 ? block.getDefaultState() : block.getStates().get(index - 1);
    }

    public void placeBlock() {
        if (closed) {
            return;
        }
        if (!matchesWorld()) {
            ChunkManager.getInstance().unregisterBlock(location);
            return;
        }
        if (respawnAt != null) {
            if (respawnAt > System.currentTimeMillis()) {
                return;
            }
            respawnAt = null;
            reward = null;
            stage = 0;
            renderStage(0);
            persist();
        } else {
            renderStage(stage);
        }
        if (face != null && reward != null) {
            showReward();
        }
    }

    public void placeNewBlock() {
        renderStage(0);
    }

    public void play(BlockFace face, ItemStack tool) {
        if (!isActive() || brushing) {
            return;
        }
        cancelTasks();
        this.face = face;
        this.tool = tool;
        brushing = true;
        if (reward == null) {
            reward = block.roll(tool);
            persist();
        }
        showReward();
        scheduleAdvance();
        particleTask =
                CustomArcheology.plugin
                        .getServer()
                        .getScheduler()
                        .runTaskTimer(
                                CustomArcheology.plugin,
                                () -> {
                                    if (isActive()) {
                                        location.getWorld()
                                                .spawnParticle(
                                                        CustomArcheology.getCorrectParticle(),
                                                        location.clone()
                                                                .add(.5, .5, .5)
                                                                .add(
                                                                        face.getDirection()
                                                                                .multiply(.5)),
                                                        5,
                                                        .1,
                                                        .1,
                                                        .1,
                                                        block.getType().createBlockData());
                                    }
                                },
                                0,
                                10);
    }

    private void scheduleAdvance() {
        long ticks =
                Math.max(
                        1L,
                        (long) (stageState(stage).getHardness() * 20 / block.getEfficiency(tool)));
        progressTask =
                CustomArcheology.plugin
                        .getServer()
                        .getScheduler()
                        .runTaskLater(
                                CustomArcheology.plugin,
                                () -> {
                                    progressTask = null;
                                    if (!brushing || !isActive()) {
                                        if (!closed && !matchesWorld()) {
                                            ChunkManager.getInstance().unregisterBlock(location);
                                        }
                                        return;
                                    }
                                    if (stage == block.getStates().size()) {
                                        finish();
                                    } else {
                                        renderStage(++stage);
                                        PacketUtil.teleportEntity(
                                                new ArrayList<>(rewardViewers),
                                                entityId + 1,
                                                rewardLocation());
                                        scheduleAdvance();
                                    }
                                },
                                ticks);
    }

    private void finish() {
        ItemStack drop = reward;
        if (drop == null) {
            return;
        }
        if (drop.hasItemMeta()) {
            ItemStack actual =
                    drop.getItemMeta()
                            .getPersistentDataContainer()
                            .get(
                                    NamespacedKeys.ARCHEOLOGY_REAL_ITEM.getNamespacedKey(),
                                    new ItemStackTagType());
            if (actual != null) {
                drop = actual;
            }
        }
        consumeDurability();
        stopSession();
        renderFinished();
        if (block.shouldRespawn()) {
            respawnAt = System.currentTimeMillis() + block.getRespawnDelayMillis();
            reward = null;
            persist();
        } else {
            ChunkManager.getInstance().unregisterBlock(location);
        }
        location.getWorld().dropItem(rewardLocation(), drop);
        location.getWorld()
                .spawnParticle(
                        CustomArcheology.getCorrectParticle(),
                        location.clone().add(.5, .5, .5),
                        100,
                        .3,
                        .3,
                        .3,
                        block.getType().createBlockData());
    }

    private void consumeDurability() {
        if (tool != null
                && tool.getItemMeta() instanceof Damageable meta
                && !meta.isUnbreakable()) {
            int damage = Math.max(0, block.getConsumeDurability());
            if (tool.getType().getMaxDurability() > 0
                    && meta.getDamage() + damage >= tool.getType().getMaxDurability()) {
                tool.setAmount(tool.getAmount() - 1);
            } else {
                meta.setDamage(meta.getDamage() + damage);
                tool.setItemMeta(meta);
            }
        }
    }

    public void pause() {
        if (!brushing || closed) {
            return;
        }
        brushing = false;
        cancelTasks();
        rollback(40);
    }

    private void rollback(long delay) {
        progressTask =
                CustomArcheology.plugin
                        .getServer()
                        .getScheduler()
                        .runTaskLater(
                                CustomArcheology.plugin,
                                () -> {
                                    progressTask = null;
                                    if (!isActive()) {
                                        return;
                                    }
                                    stage = Math.max(0, stage - 1);
                                    renderStage(stage);
                                    if (stage == 0) {
                                        hideReward();
                                        face = null;
                                        tool = null;
                                    } else {
                                        PacketUtil.teleportEntity(
                                                new ArrayList<>(rewardViewers),
                                                entityId + 1,
                                                rewardLocation());
                                        rollback(5);
                                    }
                                },
                                delay);
    }

    private Location rewardLocation() {
        return face == null
                ? location.clone().add(.5, .5, .5)
                : location.clone().add(face.getDirection().multiply(.2 * stage));
    }

    private void showReward() {
        List<Player> players = CommonUtil.getNearbyPlayers(location);
        players.removeAll(rewardViewers);
        if (players.isEmpty()) {
            return;
        }
        rewardViewers.addAll(players);
        Quaternion4f rotation =
                face == BlockFace.NORTH || face == BlockFace.SOUTH
                        ? new Quaternion4f(0, -1, 0, -1)
                        : null;
        float scale =
                (float)
                        (isArcheologyBlockItem(reward) || reward.getType().isBlock()
                                ? Config.BLOCK_SCALE.asDouble()
                                : Config.ITEM_SCALE.asDouble());
        if (rotation != null
                && CommonUtil.getMinorVersion(21, 5)
                && Config.SCALE_ISSUE_FIX.asBoolean()) {
            scale /= 2;
        }
        PacketUtil.spawnItemDisplay(
                players,
                rewardLocation(),
                reward,
                entityId + 1,
                new Vector3f(scale, scale, scale),
                rotation);
    }

    private boolean isArcheologyBlockItem(ItemStack itemStack) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        return itemMeta != null
                && itemMeta.getPersistentDataContainer().has(
                        NamespacedKeys.IS_ARCHEOLOGY_ITEM.getNamespacedKey(),
                        PersistentDataType.BOOLEAN);
    }

    private void hideReward() {
        if (!rewardViewers.isEmpty()) {
            PacketUtil.removeEntity(new ArrayList<>(rewardViewers), entityId + 1);
        }
        rewardViewers.clear();
    }

    private void cancelTasks() {
        if (progressTask != null) {
            progressTask.cancel();
        }
        if (particleTask != null) {
            particleTask.cancel();
        }
        progressTask = null;
        particleTask = null;
    }

    private void stopSession() {
        brushing = false;
        cancelTasks();
        PlayerManager.getInstance().cancelBlock(this);
        hideReward();
        tool = null;
    }

    public void prepareForChunkUnload() {
        stopSession();
        stage = 0;
        face = null;
        unloadAppearance();
        closed = true;
    }

    public void removeBlock() {
        boolean owned = matchesWorld();
        unregisterBlock();
        if (owned) {
            restoreOriginal();
        }
    }

    public void unregisterBlock() {
        stopSession();
        closed = true;
        if (!sentPlayers.isEmpty()) {
            PacketUtil.removeEntity(new ArrayList<>(sentPlayers), entityId);
        }
        sentPlayers.clear();
    }

    public void startRespawnCooldown() {
        if (!block.shouldRespawn() || closed || !matchesWorld()) {
            return;
        }
        stopSession();
        stage = 0;
        face = null;
        reward = null;
        renderFinished();
        respawnAt = System.currentTimeMillis() + block.getRespawnDelayMillis();
        persist();
    }

    private void persist() {
        ChunkManager.getInstance().getPersistentDataChunk(location).saveData();
    }
}
