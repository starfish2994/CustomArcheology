package cn.myrealm.customarcheology.mechanics.actions.types;

import cn.myrealm.customarcheology.CustomArcheology;
import cn.myrealm.customarcheology.managers.managers.ActionManager;
import cn.myrealm.customarcheology.mechanics.actions.*;
import org.bukkit.configuration.ConfigurationSection;

public class ActionChance extends AbstractRunAction {

    public ActionChance() {
        super("chance");
        setRequiredArgs("rate", "actions");
    }

    @Override
    protected void onRunAction(ActionFormat action, ActionContext context) {
        ConfigurationSection actions = action.getSection("actions");
        if (actions != null && CustomArcheology.RANDOM.nextDouble() * 100 < action.getDouble("rate", 0)) {
            ActionManager.getInstance().runActions(actions, context);
        }
    }
}
