package cn.myrealm.customarcheology.mechanics.actions.types;

import cn.myrealm.customarcheology.mechanics.actions.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

public class ActionConsoleCommand extends AbstractRunAction {

    public ActionConsoleCommand() {
        super("console_command");
        setRequiredArgs("command");
    }

    @Override
    protected void onRunAction(ActionFormat action, ActionContext context) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), action.getString("command", context));
    }

    @Override
    protected boolean onAppendLegacyAction(ConfigurationSection section, String value) {
        section.set("command", value);
        return true;
    }
}
