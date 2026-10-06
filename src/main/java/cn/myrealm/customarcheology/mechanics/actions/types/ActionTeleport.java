package cn.myrealm.customarcheology.mechanics.actions.types;

import cn.myrealm.customarcheology.mechanics.actions.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.Location;
import org.bukkit.World;

public class ActionTeleport extends AbstractRunAction {

    public ActionTeleport() {
        super("teleport"); setRequiredArgs("world", "x", "y", "z");
    }

    @Override
    protected void onRunAction(ActionFormat action, ActionContext context) {
        if (context.getPlayer() == null) {
            return;
        }
        World world = Bukkit.getWorld(action.getString("world", context));
        if (world == null) {
            return;
        }
        context.getPlayer().teleport(new Location(world, action.getDouble("x", 0), action.getDouble("y", 0), action.getDouble("z", 0), (float) action.getDouble("yaw", context.getPlayer().getLocation().getYaw()), (float) action.getDouble("pitch", context.getPlayer().getLocation().getPitch())));
    }

    @Override
    protected boolean onAppendLegacyAction(ConfigurationSection section, String value) {
        String[] parts = value.split(";;", -1);
        if (parts.length != 4 && parts.length != 6) {
            return false;
        }
        section.set("world", parts[0]);
        section.set("x", parts[1]);
        section.set("y", parts[2]);
        section.set("z", parts[3]);
        if (parts.length == 6) {
            section.set("yaw", parts[4]);
            section.set("pitch", parts[5]);
        }
        return true;
    }
}
