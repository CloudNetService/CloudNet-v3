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

package eu.cloudnetservice.modules.labymod.platform.bungeecord;

import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.network.NetworkClient;
import eu.cloudnetservice.driver.network.rpc.RPCFactory;
import eu.cloudnetservice.driver.provider.CloudServiceProvider;
import eu.cloudnetservice.driver.util.ModuleHelper;
import eu.cloudnetservice.ext.platforminject.PlatformEntrypoint;
import eu.cloudnetservice.ext.platforminject.stereotype.Dependency;
import eu.cloudnetservice.ext.platforminject.stereotype.PlatformPlugin;
import eu.cloudnetservice.modules.labymod.platform.PlatformLabyModListener;
import eu.cloudnetservice.modules.labymod.platform.PlatformLabyModManagement;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;

@Singleton
@PlatformPlugin(
  platform = "bungeecord",
  name = "CloudNet-Bridge",
  version = "{project.build.version}",
  description = "Displays LabyMod DiscordRPC information when playing on cloudnet a server",
  authors = "CloudNetService",
  dependencies = @Dependency(name = "CloudNet-Bridge"))
public class BungeeCordLabyModPlugin implements PlatformEntrypoint {

  private final Plugin plugin;
  private final ProxyServer proxy;
  private final RPCFactory rpcFactory;
  private final EventManager eventManager;
  private final ModuleHelper moduleHelper;
  private final NetworkClient networkClient;
  private final CloudServiceProvider serviceProvider;

  @Inject
  public BungeeCordLabyModPlugin(
    @NonNull Plugin plugin,
    @NonNull ProxyServer proxyServer,
    @NonNull RPCFactory rpcFactory,
    @NonNull EventManager eventManager,
    @NonNull ModuleHelper moduleHelper,
    @NonNull NetworkClient networkClient,
    @NonNull CloudServiceProvider serviceProvider
  ) {
    this.plugin = plugin;
    this.proxy = proxyServer;
    this.rpcFactory = rpcFactory;
    this.eventManager = eventManager;
    this.moduleHelper = moduleHelper;
    this.networkClient = networkClient;
    this.serviceProvider = serviceProvider;
  }

  @Override
  public void onLoad() {
    // init the labymod management
    var labyModManagement = new PlatformLabyModManagement(this.rpcFactory, this.networkClient, this.serviceProvider);
    // register the plugin channel message listener
    this.proxy.getPluginManager().registerListener(this.plugin, new BungeeCordLabyModListener(labyModManagement));
    // register the common cloudnet listener for channel messages
    this.eventManager.registerListener(PlatformLabyModListener.class);
  }

  @Override
  public void onDisable() {
    // unregister all listeners for cloudnet events
    this.moduleHelper.unregisterAll(this.getClass().getClassLoader());
  }
}
