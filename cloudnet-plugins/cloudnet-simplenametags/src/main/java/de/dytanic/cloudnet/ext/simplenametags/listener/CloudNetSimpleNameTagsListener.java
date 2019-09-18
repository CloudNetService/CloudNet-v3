package de.dytanic.cloudnet.ext.simplenametags.listener;

import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.permission.PermissionUpdateGroupEvent;
import de.dytanic.cloudnet.driver.event.events.permission.PermissionUpdateUserEvent;
import de.dytanic.cloudnet.driver.permission.IPermissionUser;
import de.dytanic.cloudnet.ext.cloudperms.CloudPermissionsPermissionManagement;
import de.dytanic.cloudnet.ext.cloudperms.bukkit.BukkitCloudNetCloudPermissionsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Optional;

public final class CloudNetSimpleNameTagsListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void handle(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("CloudNet-SimpleNameTags"), () -> BukkitCloudNetCloudPermissionsPlugin.getInstance().updateNameTags(event.getPlayer()));
    }

    @EventListener
    public void handle(PermissionUpdateUserEvent event) {
        Optional<? extends Player> optionalPlayer = Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.getUniqueId().equals(event.getPermissionUser().getUniqueId()))
                .findFirst();

        optionalPlayer.ifPresent(value -> BukkitCloudNetCloudPermissionsPlugin.getInstance().updateNameTags(value));
    }

    @EventListener
    public void handle(PermissionUpdateGroupEvent event) {
        Bukkit.getOnlinePlayers().forEach(player -> {
            IPermissionUser permissionUser = CloudPermissionsPermissionManagement.getInstance().getUser(player.getUniqueId());

            if (permissionUser != null && permissionUser.inGroup(event.getPermissionGroup().getName())) {
                BukkitCloudNetCloudPermissionsPlugin.getInstance().updateNameTags(player);
            }
        });
    }

}