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

package de.dytanic.cloudnet.driver.module;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

// TODO: docs
public interface IModuleWrapper {

  @NotNull
  @Unmodifiable Map<ModuleLifeCycle, List<IModuleTaskEntry>> getModuleTasks();

  @NotNull
  @Unmodifiable Set<ModuleDependency> getDependingModules();

  @NotNull IModule getModule();

  @NotNull ModuleLifeCycle getModuleLifeCycle();

  @NotNull IModuleProvider getModuleProvider();

  @NotNull ModuleConfiguration getModuleConfiguration();

  /**
   * @deprecated Use {@link #getModuleConfiguration()} instead - same result but unwrapped.
   */
  @Deprecated
  default JsonDocument getModuleConfigurationSource() {
    return JsonDocument.newDocument(this.getModuleConfiguration());
  }

  @NotNull ClassLoader getClassLoader();

  @NotNull IModuleWrapper loadModule();

  @NotNull IModuleWrapper startModule();

  @NotNull IModuleWrapper stopModule();

  @NotNull IModuleWrapper unloadModule();

  @Deprecated
  default File getDataFolder() {
    return this.getDataDirectory().toFile();
  }

  @NotNull Path getDataDirectory();

  @NotNull URL getUrl();

  @NotNull URI getUri();

  /**
   * @deprecated Use {@link ModuleConfiguration#getRepos()} instead.
   */
  @Deprecated
  default Map<String, String> getDefaultRepositories() {
    return Arrays.stream(this.getModuleConfiguration().getRepos())
        .collect(Collectors.toMap(ModuleRepository::getName, ModuleRepository::getUrl));
  }
}
