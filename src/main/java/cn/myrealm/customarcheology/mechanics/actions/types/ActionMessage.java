package cn.myrealm.customarcheology.mechanics.actions.types;

import cn.myrealm.customarcheology.managers.managers.system.LanguageManager;
import cn.myrealm.customarcheology.mechanics.actions.*;
import org.bukkit.configuration.ConfigurationSection;

public class ActionMessage extends AbstractRunAction {

    public ActionMessage() {
        super("message");
        setRequiredArgs("message");
    }

    @Override
    protected void onRunAction(ActionFormat action, ActionContext context) {
        if (context.getPlayer() != null) {
            context.getPlayer().sendMessage(LanguageManager.parseColor(action.getString("message", context)));
        }
    }

    @Override
    protected boolean onAppendLegacyAction(ConfigurationSection section, String value) {
        section.set("message", value);
        return true;
    }
}
