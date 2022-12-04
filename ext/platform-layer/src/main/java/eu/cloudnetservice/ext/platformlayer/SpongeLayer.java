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

package eu.cloudnetservice.ext.platformlayer;

import dev.derklaro.aerogel.BindingConstructor;
import dev.derklaro.aerogel.Bindings;
import dev.derklaro.aerogel.Element;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import java.lang.reflect.Type;
import lombok.NonNull;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.plugin.PluginManager;
import org.spongepowered.api.scheduler.Scheduler;
import org.spongepowered.plugin.PluginContainer;

public class SpongeLayer {

  public static @NonNull InjectionLayer<?> create(@NonNull PluginContainer plugin) {
    return InjectionLayer.specifiedChild(InjectionLayer.ext(), plugin.metadata().id(), (specifiedLayer, injector) -> {
      // some default bukkit bindings
      specifiedLayer.install(fixedBinding(Server.class, Sponge.server()));
      specifiedLayer.install(fixedBinding(Scheduler.class, Sponge.asyncScheduler()));
      specifiedLayer.install(fixedBinding(PluginManager.class, Sponge.pluginManager()));
      injector.installSpecified(fixedBinding(PluginContainer.class, plugin));
    });
  }

  private static @NonNull BindingConstructor fixedBinding(@NonNull Type type, @NonNull Object value) {
    return Bindings.fixed(Element.forType(type), value);
  }

}
