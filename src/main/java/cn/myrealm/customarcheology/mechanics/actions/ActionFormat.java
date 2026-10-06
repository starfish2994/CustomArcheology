package cn.myrealm.customarcheology.mechanics.actions;

import cn.myrealm.customarcheology.CustomArcheology;
import cn.myrealm.customarcheology.utils.ItemUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

public class ActionFormat {

    private final ConfigurationSection section;

    public ActionFormat(ConfigurationSection section) {
        this.section = section;
    }

    public String getType() {
        return section.getString("type", "").toLowerCase();
    }

    public boolean contains(String path) {
        return section.contains(path);
    }

    public String getString(String path, ActionContext context) {
        String value = section.getString(path);
        if (value == null) {
            return null;
        }
        Location location = context.getLocation();
        String playerName = context.getPlayer() == null ? "" : context.getPlayer().getName();
        String worldName = location.getWorld() == null ? "" : location.getWorld().getName();
        value = value.replace("{player}", playerName)
                .replace("{reward}", context.getReward() == null ? "" : ItemUtil.getItemName(context.getReward()))
                .replace("{world}", worldName)
                .replace("{x}", String.valueOf(location.getX()))
                .replace("{y}", String.valueOf(location.getY()))
                .replace("{z}", String.valueOf(location.getZ()));
        if (context.getPlayer() != null && CustomArcheology.plugin.getServer()
                .getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            value = PlaceholderAPI.setPlaceholders(context.getPlayer(), value);
        }
        return value;
    }

    public int getInt(String path, int defaultValue) {
        return section.getInt(path, defaultValue);
    }

    public boolean getBoolean(String path, boolean defaultValue) {
        return section.getBoolean(path, defaultValue);
    }

    public double getDouble(String path, double defaultValue) {
        return section.getDouble(path, defaultValue);
    }

    public ConfigurationSection getSection(String path) {
        return section.getConfigurationSection(path);
    }
}
