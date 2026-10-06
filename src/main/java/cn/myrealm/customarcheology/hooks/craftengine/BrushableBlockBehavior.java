package cn.myrealm.customarcheology.hooks.craftengine;

import cn.myrealm.customarcheology.CustomArcheology;
import cn.myrealm.customarcheology.mechanics.BrushService;
import cn.myrealm.customarcheology.mechanics.cores.ArcheologyInstance;

import net.momirealms.craftengine.bukkit.block.behavior.BukkitBlockBehavior;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.property.Property;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.world.context.UseOnContext;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

public final class BrushableBlockBehavior extends BukkitBlockBehavior {
    private final String archeologyId;
    private final Property<Integer> progress;

    public BrushableBlockBehavior(BlockDefinition block, ConfigSection config) {
        super(block);
        archeologyId = config.getNonEmptyString("archeology-id");
        progress =
                BlockBehaviorFactory.getProperty(
                        "customarcheology:brushable",
                        block,
                        config.getString("progress-property", "dusted"),
                        Integer.class);
        if (!progress.possibleValues().contains(0)) {
            throw new IllegalArgumentException("Progress property must include 0");
        }
    }

    public String archeologyId() {
        return archeologyId;
    }

    public Property<Integer> progress() {
        return progress;
    }

    @Override
    public InteractionResult useOnBlock(UseOnContext context, ImmutableBlockState state) {
        if (!CustomArcheology.plugin.isEnabled()
                || !CraftEngineSupport.isReady()
                || context.getPlayer() == null
                || context.getHand() != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        Player player = (Player) context.getPlayer().platformEntity();
        if (player.getInventory().getItemInMainHand().getType() != Material.BRUSH) {
            return InteractionResult.PASS;
        }
        var pos = context.getClickedPos();
        Location location = new Location(player.getWorld(), pos.x(), pos.y(), pos.z());
        ArcheologyInstance instance = CraftEngineSupport.attach(location);
        if (instance == null) {
            return InteractionResult.PASS;
        }
        boolean started =
                BrushService.start(
                        player,
                        instance,
                        BlockFace.valueOf(context.getClickedFace().name()),
                        player.getInventory().getItemInMainHand());
        // PASS allows vanilla brush use to start, including the release-use packet on interruption.
        return started ? InteractionResult.PASS : InteractionResult.FAIL;
    }
}
