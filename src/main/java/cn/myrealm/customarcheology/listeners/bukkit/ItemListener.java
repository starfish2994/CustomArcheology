package cn.myrealm.customarcheology.listeners.bukkit;


import cn.myrealm.customarcheology.enums.NamespacedKeys;
import cn.myrealm.customarcheology.listeners.BaseListener;
import cn.myrealm.customarcheology.managers.managers.ActionManager;
import cn.myrealm.customarcheology.mechanics.actions.ActionContext;
import cn.myrealm.customarcheology.mechanics.persistent_data.StringArrayTagType;
import cn.myrealm.customarcheology.utils.CommonUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * @author rzt1020
 */
public class ItemListener extends BaseListener {
    public ItemListener(JavaPlugin plugin) {
        super(plugin);
    }

    @EventHandler
    public void onItemSpawnEntity(ItemSpawnEvent event) {
        ItemMeta meta = event.getEntity().getItemStack().getItemMeta();
        StringArrayTagType strArray = new StringArrayTagType(StandardCharsets.UTF_8);
        if (Objects.nonNull(meta) && meta.getPersistentDataContainer().has(
                NamespacedKeys.ARCHEOLOGY_EXECUTE_ACTIONS_SPAWN.getNamespacedKey(), strArray)) {
            String[] actions = meta.getPersistentDataContainer().get(
                    NamespacedKeys.ARCHEOLOGY_EXECUTE_ACTIONS_SPAWN.getNamespacedKey(), strArray);
            if (Objects.nonNull(actions)) {
                for (int i = 0; i < event.getEntity().getItemStack().getAmount(); i++) {
                    ActionManager.getInstance().runSerializedActions(actions,
                            new ActionContext(null, event.getLocation(), event.getEntity().getItemStack()));
                }
            }
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ItemMeta meta = event.getItem().getItemStack().getItemMeta();
        StringArrayTagType strArray = new StringArrayTagType(StandardCharsets.UTF_8);
        if (Objects.nonNull(meta) && meta.getPersistentDataContainer().has(NamespacedKeys.ARCHEOLOGY_EXECUTE_ACTIONS_PICK.getNamespacedKey(), strArray)) {
            String[] actions = meta.getPersistentDataContainer().get(NamespacedKeys.ARCHEOLOGY_EXECUTE_ACTIONS_PICK.getNamespacedKey(), strArray);
            if (Objects.nonNull(actions)) {
                for (int i = 0; i < event.getItem().getItemStack().getAmount(); i++) {
                    ActionManager.getInstance().runSerializedActions(actions,
                            new ActionContext(player, event.getItem().getLocation(), event.getItem().getItemStack()));
                }
            }
            event.getItem().remove();
            event.setCancelled(true);
        }
    }
}
