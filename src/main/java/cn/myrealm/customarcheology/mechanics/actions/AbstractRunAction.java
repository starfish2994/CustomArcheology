package cn.myrealm.customarcheology.mechanics.actions;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractRunAction {

    private static final Pattern LEGACY_CHANCE = Pattern.compile("~(\\d+)$");

    private final String type;

    private String[] requiredArgs;

    protected AbstractRunAction(String type) {
        this.type = type;
    }

    protected void setRequiredArgs(String... requiredArgs) {
        this.requiredArgs = requiredArgs;
    }

    public final void runAction(ActionFormat action, ActionContext context) {
        if (requiredArgs != null) {
            for (String arg : requiredArgs) {
                if (!action.contains(arg)) {
                    Bukkit.getConsoleSender().sendMessage("§x§9§8§F§B§9§8[CustomArcheology] §cError: Action '" + type + "' is missing required option '" + arg + "'.");
                    return;
                }
            }
        }
        onRunAction(action, context);
    }

    public final boolean appendLegacyAction(ConfigurationSection section, String value) {
        Matcher chanceMatcher = LEGACY_CHANCE.matcher(value);
        if (chanceMatcher.find()) {
            section.set("type", "chance");
            section.set("rate", Integer.parseInt(chanceMatcher.group(1)));
            return appendLegacyAction(section.createSection("actions").createSection("1"),
                    value.substring(0, chanceMatcher.start()));
        }
        section.set("type", type);
        return onAppendLegacyAction(section, value);
    }

    protected boolean onAppendLegacyAction(ConfigurationSection section, String value) {
        return false;
    }

    protected abstract void onRunAction(ActionFormat action, ActionContext context);

    public String getType() {
        return type;
    }
}
