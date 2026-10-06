package cn.myrealm.customarcheology.mechanics.actions.types;

import cn.myrealm.customarcheology.mechanics.actions.*;
import org.bukkit.configuration.ConfigurationSection;

public class ActionSound extends AbstractRunAction {

    public ActionSound() {
        super("sound");
        setRequiredArgs("sound");
    }

    @Override
    protected void onRunAction(ActionFormat action, ActionContext context) {
        if (context.getPlayer() != null) {
            context.getPlayer().playSound(context.getPlayer().getLocation(), action.getString("sound", context), (float) action.getDouble("volume", 1), (float) action.getDouble("pitch", 1));
        }
    }

    @Override
    protected boolean onAppendLegacyAction(ConfigurationSection section, String value) {
        String[] parts = value.split(";;", -1);
        section.set("sound", parts[0]);
        if (parts.length > 1) {
            section.set("volume", parts[1]);
        }
        if (parts.length > 2) {
            section.set("pitch", parts[2]);
        }
        return true;
    }
}
