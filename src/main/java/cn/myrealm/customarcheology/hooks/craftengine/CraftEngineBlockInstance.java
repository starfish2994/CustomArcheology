package cn.myrealm.customarcheology.hooks.craftengine;

import cn.myrealm.customarcheology.mechanics.cores.ArcheologyInstance;
import cn.myrealm.customarcheology.mechanics.cores.BlockMode;

import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.util.Key;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

public final class CraftEngineBlockInstance extends ArcheologyInstance {
    private final String ceId;
    private final String originalState;
    private String expectedState;

    public CraftEngineBlockInstance(
            String name,
            Location location,
            ItemStack reward,
            Long respawnAt,
            String ceId,
            String expectedState,
            String originalState) {
        super(name, location, reward, respawnAt);
        this.ceId = ceId;
        this.expectedState = expectedState;
        this.originalState = originalState;
    }

    @Override
    public BlockMode getMode() {
        return BlockMode.CRAFTENGINE;
    }

    @Override
    public String getBackendId() {
        return ceId;
    }

    @Override
    public String getExpectedState() {
        return expectedState == null ? "" : expectedState;
    }

    @Override
    public String getOriginalState() {
        return originalState == null ? "" : originalState;
    }

    @Override
    public boolean isValid() {
        return super.isValid() && CraftEngineSupport.validDefinition(ceId, block)
                && CraftEngineSupport.validReplaceDefinition(block);
    }

    @Override
    protected void renderStage(int stage) {
        var definition = CraftEngineBlocks.byId(Key.of(ceId));
        ImmutableBlockState current = CraftEngineBlocks.getCustomBlockState(location.getBlock());
        var base =
                current != null && current.owner().value().id().toString().equals(ceId)
                        ? current
                        : definition.defaultState();
        var behavior = definition.defaultState().behavior().getFirst(BrushableBlockBehavior.class);
        var next = base.with(behavior.progress(), stage);
        if (next != current && !CraftEngineBlocks.place(location, next, false)) {
            throw new IllegalStateException("Could not place CE block at " + location);
        }
        expectedState = fingerprint();
    }

    @Override
    protected void renderFinished() {
        String finished = block.getCraftEngineFinishedBlock();
        var custom = CraftEngineBlocks.byId(Key.of(finished));
        if (custom != null) {
            if (!CraftEngineBlocks.place(location, custom.defaultState(), false)) {
                throw new IllegalStateException("Could not place finished CE block: " + finished);
            }
        } else {
            CraftEngineBlocks.remove(location.getBlock(), false);
            location.getBlock().setBlockData(Bukkit.createBlockData(finished));
        }
        expectedState = fingerprint();
    }

    @Override
    protected void restoreOriginal() {
        CraftEngineSupport.restoreReplaceBlock(location, block, originalState);
    }

    @Override
    protected void unloadAppearance() {
        if (CraftEngineSupport.isReady() && isValid() && !isCoolingDown() && matchesWorld()) {
            renderStage(0);
        }
    }

    @Override
    protected boolean matchesWorld() {
        if (!CraftEngineSupport.isReady()) {
            return true;
        }
        if (isCoolingDown()) {
            return expectedState != null && expectedState.equals(fingerprint());
        }
        var state = CraftEngineBlocks.getCustomBlockState(location.getBlock());
        return state != null && state.owner().value().id().toString().equals(ceId);
    }

    @Override
    public void placeBlock() {
        if (CraftEngineSupport.isReady() && isValid()) {
            super.placeBlock();
        }
    }

    @Override
    public boolean isActive() {
        return CraftEngineSupport.isReady() && isValid() && super.isActive();
    }

    private String fingerprint() {
        var state = CraftEngineBlocks.getCustomBlockState(location.getBlock());
        return state == null
                ? location.getBlock().getBlockData().getAsString()
                : "ce:" + state.toString();
    }
}
