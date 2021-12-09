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

package eu.cloudnetservice.cloudnet.ext.syncproxy.platform.listener;

import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.channel.ChannelMessageReceiveEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceLifecycleChangeEvent;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceUpdateEvent;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import eu.cloudnetservice.cloudnet.ext.syncproxy.SyncProxyConstants;
import eu.cloudnetservice.cloudnet.ext.syncproxy.config.SyncProxyConfiguration;
import eu.cloudnetservice.cloudnet.ext.syncproxy.platform.PlatformSyncProxyManagement;
import org.jetbrains.annotations.NotNull;

public final class SyncProxyCloudListener<P> {

  private final PlatformSyncProxyManagement<P> management;

  public SyncProxyCloudListener(@NotNull PlatformSyncProxyManagement<P> management) {
    this.management = management;
  }

  @EventListener
  public void handleServiceLifecycleChange(@NotNull CloudServiceLifecycleChangeEvent event) {
    if (event.getNewLifeCycle() == ServiceLifeCycle.RUNNING) {
      // notify the players about a new service start
      this.notifyPlayers("start-service", event.getServiceInfo());
    } else if (event.getNewLifeCycle() == ServiceLifeCycle.STOPPED) {
      // notify the players about the service stop
      this.notifyPlayers("stop-service", event.getServiceInfo());
      // remove the ServiceInfoSnapshot from the cache as the service is stopping
      this.management.removeCachedServiceInfoSnapshot(event.getServiceInfo());
    }
  }

  @EventListener
  public void handleServiceUpdate(@NotNull CloudServiceUpdateEvent event) {
    // check if the service is not stopping, as this would lead to issues with the CloudServiceLifecycleChangeEvent
    if (event.getServiceInfo().getLifeCycle() != ServiceLifeCycle.STOPPED) {
      // cache the ServiceInfoSnapshot
      this.management.cacheServiceInfoSnapshot(event.getServiceInfo());
    }
  }

  @EventListener
  public void handleConfigUpdate(@NotNull ChannelMessageReceiveEvent event) {
    // handle incoming channel messages on the syncproxy channel
    if (event.getChannel().equals(SyncProxyConstants.SYNC_PROXY_CHANNEL)
      && SyncProxyConstants.SYNC_PROXY_UPDATE_CONFIG.equals(event.getMessage())) {
      // update the configuration locally
      this.management.setConfigurationSilently(event.getContent().readObject(SyncProxyConfiguration.class));
    }
  }

  private void notifyPlayers(@NotNull String key, @NotNull ServiceInfoSnapshot serviceInfoSnapshot) {
    // only message the players if we are supposed to
    if (!this.management.getConfiguration().showIngameServicesStartStopMessages()) {
      return;
    }
    for (P onlinePlayer : this.management.getOnlinePlayers()) {
      if (this.management.checkPlayerPermission(onlinePlayer, "cloudnet.syncproxy.notify")) {
        this.management.messagePlayer(onlinePlayer, this.management.getServiceUpdateMessage(key, serviceInfoSnapshot));
      }
    }
  }
}