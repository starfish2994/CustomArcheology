package cn.myrealm.customarcheology.mechanics.actions;

import cn.myrealm.customarcheology.CustomArcheology;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ActionLocationUtil {
    private ActionLocationUtil() {
    }

    public static Location findSafeLocation(Location origin) {
        World world = origin.getWorld();
        if (world == null) {
            return null;
        }
        int originX = origin.getBlockX();
        int originY = origin.getBlockY();
        int originZ = origin.getBlockZ();
        for (int radius = 0; radius <= 4; radius++) {
            List<Location> locations = new ArrayList<>();
            for (int x = originX - radius; x <= originX + radius; x++) {
                for (int z = originZ - radius; z <= originZ + radius; z++) {
                    if (Math.max(Math.abs(x - originX), Math.abs(z - originZ)) != radius) {
                        continue;
                    }
                    for (int y = originY - 3; y <= originY + 4; y++) {
                        Block feet = world.getBlockAt(x, y, z);
                        Block head = feet.getRelative(0, 1, 0);
                        Block ground = feet.getRelative(0, -1, 0);
                        if (feet.isPassable() && head.isPassable() && ground.getType().isSolid() && !ground.isLiquid()) {
                            locations.add(new Location(world, x + .5, y, z + .5));
                        }
                    }
                }
            }
            if (!locations.isEmpty()) {
                Collections.shuffle(locations, CustomArcheology.RANDOM);
                return locations.get(0);
            }
        }
        return null;
    }
}
