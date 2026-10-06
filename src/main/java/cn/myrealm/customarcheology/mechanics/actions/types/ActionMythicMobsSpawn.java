package cn.myrealm.customarcheology.mechanics.actions.types;

import cn.myrealm.customarcheology.mechanics.actions.*;
import cn.myrealm.customarcheology.utils.CommonUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

public class ActionMythicMobsSpawn extends AbstractRunAction {
    public ActionMythicMobsSpawn() {
        super("mythicmobs_spawn");
        setRequiredArgs("entity");
    }

    @Override
    protected void onRunAction(ActionFormat action, ActionContext context) {
        Location location = context.getLocation();
        if (action.contains("world") && action.contains("x") && action.contains("y") && action.contains("z")) {
            World world = Bukkit.getWorld(action.getString("world", context));
            if (world == null) {
                return;
            }
            location = new Location(world, action.getDouble("x", 0), action.getDouble("y", 0), action.getDouble("z", 0));
        }
        Location safeLocation = ActionLocationUtil.findSafeLocation(location);
        if (safeLocation != null) {
            CommonUtil.summonMythicMobs(safeLocation, action.getString("entity", context), action.getInt("level", 1));
        }
    }

    @Override
    protected boolean onAppendLegacyAction(ConfigurationSection section, String value) {
        String[] parts = value.split(";;", -1);
        if (parts.length != 1 && parts.length != 2 && parts.length != 5 && parts.length != 6) {
            return false;
        }
        section.set("entity", parts[0]);
        if (parts.length == 2 || parts.length == 6) {
            section.set("level", parts[1]);
        }
        if (parts.length == 5 || parts.length == 6) {
            int offset = parts.length == 6 ? 2 : 1;
            section.set("world", parts[offset]);
            section.set("x", parts[offset + 1]);
            section.set("y", parts[offset + 2]);
            section.set("z", parts[offset + 3]);
        }
        return true;
    }
}
