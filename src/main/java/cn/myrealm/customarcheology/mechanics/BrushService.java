package cn.myrealm.customarcheology.mechanics;

import cn.myrealm.customarcheology.enums.Messages;
import cn.myrealm.customarcheology.enums.Permissions;
import cn.myrealm.customarcheology.managers.managers.HookManager;
import cn.myrealm.customarcheology.managers.managers.PlayerManager;
import cn.myrealm.customarcheology.mechanics.cores.ArcheologyInstance;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class BrushService {

    private BrushService() {}

    public static boolean start(
            Player player, ArcheologyInstance block, BlockFace face, ItemStack item) {
        if (block == null
                || !block.isActive()
                || item == null
                || item.getType() != Material.BRUSH) {
            return false;
        }
        if (player.getGameMode() != GameMode.SURVIVAL
                && player.getGameMode() != GameMode.CREATIVE) {
            return false;
        }
        if (!Permissions.PLAY_ARCHEOLOGY.hasPermission(player)) {
            player.sendMessage(Messages.GAME_BRUSH_NO_PERMISSION.getMessageWithPrefix());
            return false;
        }
        if (!HookManager.getHookManager().getProtectionCanBreak(player, block.getLocation())) {
            return false;
        }
        if (!block.getArcheologyBlock().canBrush(item)) {
            player.sendMessage(Messages.GAME_CAN_NOT_BRUSH.getMessageWithPrefix());
            return false;
        }
        PlayerManager.getInstance().setBrush(player, block, face, item);
        return true;
    }
}
