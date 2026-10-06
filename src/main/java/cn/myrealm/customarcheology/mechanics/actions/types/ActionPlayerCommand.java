package cn.myrealm.customarcheology.mechanics.actions.types;

import cn.myrealm.customarcheology.mechanics.actions.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

public class ActionPlayerCommand extends AbstractRunAction {

    public ActionPlayerCommand() {
        super("player_command");
        setRequiredArgs("command");
    }

    @Override
    protected void onRunAction(ActionFormat action, ActionContext context) {
        if (context.getPlayer() != null) {
            Bukkit.dispatchCommand(context.getPlayer(), action.getString("command", context));
        }
    }

    @Override
    protected boolean onAppendLegacyAction(ConfigurationSection section, String value) {
        section.set("command", value);
        return true;
    }
}
