/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.syncproxy.platform.velocity;

import com.google.inject.Inject;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.driver.util.ModuleHelper;
import eu.cloudnetservice.ext.platforminject.PlatformEntrypoint;
import eu.cloudnetservice.ext.platforminject.stereotype.Dependency;
import eu.cloudnetservice.ext.platforminject.stereotype.PlatformPlugin;
import eu.cloudnetservice.modules.syncproxy.platform.listener.SyncProxyCloudListener;
import lombok.NonNull;

@PlatformPlugin(
  platform = "velocity",
  name = "CloudNet-SyncProxy",
  version = "{project.build.version}",
  description = "CloudNet extension which serves proxy utils with CloudNet support",
  authors = "CloudNetService",
  dependencies = {@Dependency(name = "cloudnet_bridge"), @Dependency(name = "cloudnet_cloudperms", optional = true)})
public final class VelocitySyncProxyPlugin implements PlatformEntrypoint {

  private final EventManager eventManager;
  private final ModuleHelper moduleHelper;
  private final ServiceRegistry serviceRegistry;
  private final VelocitySyncProxyManagement syncProxyManagement;

  @Inject
  public VelocitySyncProxyPlugin(
    @NonNull EventManager eventManager,
    @NonNull ModuleHelper moduleHelper,
    @NonNull ServiceRegistry serviceRegistry,
    @NonNull VelocitySyncProxyManagement syncProxyManagement
  ) {
    this.eventManager = eventManager;
    this.moduleHelper = moduleHelper;
    this.serviceRegistry = serviceRegistry;
    this.syncProxyManagement = syncProxyManagement;
  }

  @Override
  public void onLoad() {
    // register the SyncProxyManagement in our service registry
    this.syncProxyManagement.registerService(this.serviceRegistry);
    // register the event listener to handle service updates
    this.eventManager.registerListener(new SyncProxyCloudListener<>(this.syncProxyManagement));
  }

  private void registerListener(
    @NonNull com.velocitypowered.api.event.EventManager eventManager,
    @NonNull VelocitySyncProxyListener listener
  ) {
    eventManager.register(this, listener);
  }

  @Override
  public void onDisable() {
    this.moduleHelper.unregisterAll(this.getClass().getClassLoader());
  }
}
