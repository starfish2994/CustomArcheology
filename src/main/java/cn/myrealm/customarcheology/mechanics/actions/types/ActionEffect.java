package cn.myrealm.customarcheology.mechanics.actions.types;

import cn.myrealm.customarcheology.mechanics.actions.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class ActionEffect extends AbstractRunAction {

    public ActionEffect() {
        super("effect");
        setRequiredArgs("potion", "duration", "level");
    }

    @Override
    protected void onRunAction(ActionFormat action, ActionContext context) {
        if (context.getPlayer() == null) {
            return;
        }
        PotionEffectType type = PotionEffectType.getByName(action.getString("potion", context).toUpperCase());
        if (type != null) {
            context.getPlayer().addPotionEffect(new PotionEffect(type, action.getInt("duration", 0), action.getInt("level", 1) - 1, action.getBoolean("ambient", true), action.getBoolean("particles", true), action.getBoolean("icon", true)));
        }
    }

    @Override
    protected boolean onAppendLegacyAction(ConfigurationSection section, String value) {
        String[] parts = value.split(";;", -1);
        if (parts.length != 3) {
            return false;
        }
        section.set("potion", parts[0]);
        section.set("level", parts[1]);
        section.set("duration", parts[2]);
        return true;
    }
}
