package de.dytanic.cloudnet.ext.signs.bukkit;

import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import de.dytanic.cloudnet.ext.signs.Sign;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class BukkitSignKnockbackRunnable implements Runnable {

  private final Map<WorldPosition, Location> signLocations = new HashMap<>();

  private final BukkitSignManagement bukkitSignManagement;

  BukkitSignKnockbackRunnable(BukkitSignManagement bukkitSignManagement) {
    this.bukkitSignManagement = bukkitSignManagement;
  }

  @Override
  public void run() {
    Set<Sign> signs = this.bukkitSignManagement.getSigns();

    for (Sign sign : signs) {
      Location signLocation = this.signLocations
        .computeIfAbsent(sign.getWorldPosition(), this.bukkitSignManagement::toLocation);

      if (signLocation != null && signLocation.getWorld() != null) {
        double knockbackDistance = this.bukkitSignManagement.getOwnSignConfigurationEntry().getKnockbackDistance();

        signLocation.getWorld()
          .getNearbyEntities(signLocation, knockbackDistance, knockbackDistance, knockbackDistance)
          .stream()
          .filter(entity -> entity instanceof Player && !entity.hasPermission("cloudnet.signs.knockback.bypass"))
          .forEach(player -> {
            // pushing the player back with the specified strength
            player.setVelocity(player.getLocation().toVector().subtract(signLocation.toVector())
              .normalize()
              .multiply(this.bukkitSignManagement.getOwnSignConfigurationEntry().getKnockbackStrength())
              .setY(0.2));
          });
      }
    }

  }

}
