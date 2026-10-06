package cn.myrealm.customarcheology.mechanics.cores;

import cn.myrealm.customarcheology.hooks.craftengine.CraftEngineSupport;
import cn.myrealm.customarcheology.utils.CommonUtil;
import cn.myrealm.customarcheology.utils.PacketUtil;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Legacy barrier block with a packet-only model. */
public final class FakeTileBlock extends ArcheologyInstance {
    private int displayedStage = -1;

    public FakeTileBlock(String name, Location location, ItemStack reward, Long respawnAt) {
        super(name, location, reward, respawnAt);
    }

    @Override
    public BlockMode getMode() {
        return BlockMode.LEGACY;
    }

    @Override
    protected void renderStage(int stage) {
        location.getBlock().setType(Material.BARRIER);
        List<Player> nearby = CommonUtil.getNearbyPlayers(location);
        nearby.removeAll(sentPlayers);
        if (!nearby.isEmpty()) {
            PacketUtil.spawnItemDisplay(
                    nearby,
                    location,
                    block.generateLegacyItemStack(1, stageState(stage)),
                    entityId,
                    null,
                    null);
            sentPlayers.addAll(nearby);
        }
        if (displayedStage != stage && !sentPlayers.isEmpty()) {
            PacketUtil.updateItemDisplay(
                    new ArrayList<>(sentPlayers),
                    block.generateLegacyItemStack(1, stageState(stage)),
                    entityId,
                    null,
                    null);
        }
        displayedStage = stage;
    }

    @Override
    protected void renderFinished() {
        hide();
        location.getBlock().setType(block.getFinishedState().getMaterial());
    }

    private void hide() {
        if (!sentPlayers.isEmpty()) {
            PacketUtil.removeEntity(new ArrayList<>(sentPlayers), entityId);
        }
        sentPlayers.clear();
        displayedStage = -1;
    }

    @Override
    protected void restoreOriginal() {
        if (block.getCraftEngineReplaceBlock() == null) {
            location.getBlock().setType(block.getType());
        } else {
            CraftEngineSupport.restoreReplaceBlock(location, block, "");
        }
    }

    @Override
    protected void unloadAppearance() {
        hide();
        if (matchesWorld()) {
            if (isCoolingDown()) {
                location.getBlock().setType(block.getFinishedState().getMaterial());
            } else {
                restoreOriginal();
            }
        }
    }

    @Override
    protected boolean matchesWorld() {
        return isCoolingDown()
                ? location.getBlock().getType() == block.getFinishedState().getMaterial()
                : location.getBlock().getType() == Material.BARRIER
                        || block.matchesReplaceBlock(location.getBlock());
    }
}
