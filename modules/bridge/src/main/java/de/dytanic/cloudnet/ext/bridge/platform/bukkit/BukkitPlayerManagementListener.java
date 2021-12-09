/*
 * Copyright 2019-2021 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.dytanic.cloudnet.ext.bridge.platform.bukkit;

import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.ext.bridge.platform.PlatformBridgeManagement;
import de.dytanic.cloudnet.ext.bridge.platform.helper.ServerPlatformHelper;
import de.dytanic.cloudnet.wrapper.Wrapper;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

final class BukkitPlayerManagementListener implements Listener {

  private final Plugin plugin;
  private final PlatformBridgeManagement<?, ?> management;

  public BukkitPlayerManagementListener(@NotNull Plugin plugin, @NotNull PlatformBridgeManagement<?, ?> management) {
    this.plugin = plugin;
    this.management = management;
  }

  @EventHandler
  public void handle(@NotNull PlayerLoginEvent event) {
    ServiceTask task = this.management.getSelfTask();
    // check if the current task is present
    if (task != null) {
      // check if maintenance is activated
      if (task.isMaintenance() && !event.getPlayer().hasPermission("cloudnet.bridge.maintenance")) {
        event.setResult(Result.KICK_WHITELIST);
        event.setKickMessage(this.management.getConfiguration().getMessage(
          Locale.forLanguageTag(BukkitUtil.getPlayerLocale(event.getPlayer())),
          "server-join-cancel-because-maintenance"));
        return;
      }
      // check if a custom permission is required to join
      String permission = task.getProperties().getString("requiredPermission");
      if (permission != null && !event.getPlayer().hasPermission(permission)) {
        event.setResult(Result.KICK_WHITELIST);
        event.setKickMessage(this.management.getConfiguration().getMessage(
          Locale.forLanguageTag(BukkitUtil.getPlayerLocale(event.getPlayer())),
          "server-join-cancel-because-permission"));
      }
    }
  }

  @EventHandler
  public void handle(@NotNull PlayerJoinEvent event) {
    ServerPlatformHelper.sendChannelMessageLoginSuccess(
      event.getPlayer().getUniqueId(),
      this.management.getOwnNetworkServiceInfo());
    // update the service info in the next tick
    Bukkit.getScheduler().runTask(this.plugin, () -> Wrapper.getInstance().publishServiceInfoUpdate());
  }

  @EventHandler
  public void handle(@NotNull PlayerQuitEvent event) {
    ServerPlatformHelper.sendChannelMessageDisconnected(
      event.getPlayer().getUniqueId(),
      this.management.getOwnNetworkServiceInfo());
    // update the service info in the next tick
    Bukkit.getScheduler().runTask(this.plugin, () -> Wrapper.getInstance().publishServiceInfoUpdate());
  }
}