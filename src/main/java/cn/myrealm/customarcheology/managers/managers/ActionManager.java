package cn.myrealm.customarcheology.managers.managers;

import cn.myrealm.customarcheology.managers.BaseManager;
import cn.myrealm.customarcheology.mechanics.actions.AbstractRunAction;
import cn.myrealm.customarcheology.mechanics.actions.ActionContext;
import cn.myrealm.customarcheology.mechanics.actions.ActionFormat;
import cn.myrealm.customarcheology.mechanics.actions.types.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ActionManager extends BaseManager {

    private static ActionManager instance;

    private Map<String, AbstractRunAction> actions;

    public ActionManager(JavaPlugin plugin) {
        super(plugin);
        instance = this;
    }

    @Override
    protected void onInit() {
        actions = new LinkedHashMap<>();
        registerNewAction(new ActionMessage());
        registerNewAction(new ActionAnnouncement());
        registerNewAction(new ActionSound());
        registerNewAction(new ActionEffect());
        registerNewAction(new ActionTeleport());
        registerNewAction(new ActionConsoleCommand());
        registerNewAction(new ActionPlayerCommand());
        registerNewAction(new ActionOpCommand());
        registerNewAction(new ActionEntitySpawn());
        registerNewAction(new ActionMythicMobsSpawn());
        registerNewAction(new ActionChance());
    }

    public void registerNewAction(AbstractRunAction action) {
        actions.putIfAbsent(action.getType(), action);
    }

    public void runActions(ConfigurationSection section, ActionContext context) {
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection actionSection = section.getConfigurationSection(id);
            if (actionSection != null) {
                runAction(new ActionFormat(actionSection), context);
            }
        }
    }

    public void runAction(ActionFormat action, ActionContext context) {
        AbstractRunAction runAction = actions.get(action.getType());
        if (runAction == null) {
            plugin.getLogger().warning("Unknown action type: " + action.getType());
            return;
        }
        runAction.runAction(action, context);
    }

    public String serializeActions(ConfigurationSection section) {
        if (section == null || section.getKeys(false).isEmpty()) {
            return null;
        }
        YamlConfiguration serialized = new YamlConfiguration();
        copySection(section, serialized);
        return serialized.saveToString();
    }

    public String serializeActions(ConfigurationSection section, List<String> legacyActions) {
        String serializedActions = serializeActions(section);
        if (serializedActions != null) {
            return serializedActions;
        }
        if (legacyActions == null || legacyActions.isEmpty()) {
            return null;
        }
        YamlConfiguration serialized = new YamlConfiguration();
        int index = 1;
        for (String legacyAction : legacyActions) {
            if (appendLegacyAction(serialized.createSection(String.valueOf(index)), legacyAction)) {
                index++;
            } else {
                serialized.set(String.valueOf(index), null);
                plugin.getLogger().warning("Unknown legacy action: " + legacyAction);
            }
        }
        return index == 1 ? null : serialized.saveToString();
    }

    public void runSerializedActions(String[] serializedActions, ActionContext context) {
        if (serializedActions == null) {
            return;
        }
        for (String serializedAction : serializedActions) {
            runActions(YamlConfiguration.loadConfiguration(new StringReader(serializedAction)), context);
        }
    }

    private void copySection(ConfigurationSection source, ConfigurationSection target) {
        for (String key : source.getKeys(false)) {
            ConfigurationSection child = source.getConfigurationSection(key);
            if (child != null) {
                ConfigurationSection targetChild = target.createSection(key);
                copySection(child, targetChild);
            } else {
                target.set(key, source.get(key));
            }
        }
    }

    private boolean appendLegacyAction(ConfigurationSection section, String legacyAction) {
        int separator = legacyAction.indexOf(':');
        if (separator < 0) {
            return false;
        }
        String type = legacyAction.substring(0, separator).trim().toLowerCase();
        String value = legacyAction.substring(separator + 1).trim();
        AbstractRunAction action = actions.get(type);
        return action != null && action.appendLegacyAction(section, value);
    }

    public static ActionManager getInstance() {
        return instance;
    }
}
