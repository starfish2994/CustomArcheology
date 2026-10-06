package cn.myrealm.customarcheology.mechanics.actions.types;

import cn.myrealm.customarcheology.mechanics.actions.*;
import cn.myrealm.customarcheology.utils.CommonUtil;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;

public class ActionEntitySpawn extends AbstractRunAction {
    public ActionEntitySpawn() {
        super("entity_spawn");
        setRequiredArgs("entity");
    }

    @Override
    protected void onRunAction(ActionFormat action, ActionContext context) {
        EntityType entity = Registry.ENTITY_TYPE.get(CommonUtil.parseNamespacedKey(action.getString("entity", context)));
        if (entity == null) {
            return;
        }
        Location origin = context.getLocation();
        if (action.contains("world") && action.contains("x") && action.contains("y") && action.contains("z")) {
            World world = Bukkit.getWorld(action.getString("world", context));
            if (world == null) {
                return;
            }
            origin = new Location(world, action.getDouble("x", 0), action.getDouble("y", 0), action.getDouble("z", 0));
        }
        Location location = ActionLocationUtil.findSafeLocation(origin);
        if (location != null) {
            location.getWorld().spawnEntity(location, entity);
        }
    }

    @Override
    protected boolean onAppendLegacyAction(ConfigurationSection section, String value) {
        String[] parts = value.split(";;", -1);
        if (parts.length != 1 && parts.length != 5) {
            return false;
        }
        section.set("entity", parts[0]);
        if (parts.length == 5) {
            section.set("world", parts[1]);
            section.set("x", parts[2]);
            section.set("y", parts[3]);
            section.set("z", parts[4]);
        }
        return true;
    }
}
