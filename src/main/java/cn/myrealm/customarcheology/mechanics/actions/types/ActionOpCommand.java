package cn.myrealm.customarcheology.mechanics.actions.types;

import cn.myrealm.customarcheology.mechanics.actions.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

public class ActionOpCommand extends AbstractRunAction {

    public ActionOpCommand() {
        super("op_command");
        setRequiredArgs("command");
    }

    @Override
    protected void onRunAction(ActionFormat action, ActionContext context) {
        if (context.getPlayer() == null) {
            return;
        }
        boolean op = context.getPlayer().isOp();
        try {
            context.getPlayer().setOp(true);
            Bukkit.dispatchCommand(context.getPlayer(), action.getString("command", context));
        }
        finally { context.getPlayer().setOp(op); }
    }

    @Override
    protected boolean onAppendLegacyAction(ConfigurationSection section, String value) {
        section.set("command", value);
        return true;
    }
}
