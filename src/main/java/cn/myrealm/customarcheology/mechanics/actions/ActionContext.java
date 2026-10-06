package cn.myrealm.customarcheology.mechanics.actions;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ActionContext {

    private final Player player;

    private final Location location;

    private final ItemStack reward;

    public ActionContext(Player player, Location location, ItemStack reward) {
        this.player = player;
        this.location = location;
        this.reward = reward;
    }

    public Player getPlayer() {
        return player;
    }

    public Location getLocation() {
        return location;
    }

    public ItemStack getReward() {
        return reward;
    }
}
