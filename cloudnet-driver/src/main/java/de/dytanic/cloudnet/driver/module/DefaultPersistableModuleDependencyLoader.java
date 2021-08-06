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

import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.common.io.HttpConnectionProvider;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

public class DefaultPersistableModuleDependencyLoader extends DefaultMemoryModuleDependencyLoader {

  // format: <name>-<version>.jar
  protected static final String FILE_NAME_FORMAT = "%s-%s.jar";

  protected final Path baseDirectory;

  public DefaultPersistableModuleDependencyLoader(Path baseDirectory) {
    this.baseDirectory = baseDirectory;
    FileUtils.createDirectoryReported(baseDirectory);
  }

  @Override
  public @NotNull URL loadModuleDependencyByUrl(
    @NotNull ModuleConfiguration configuration,
    @NotNull ModuleDependency dependency
  ) throws Exception {
    URL memoryBasedUrl = super.loadModuleDependencyByUrl(configuration, dependency);
    return this.loadDependency(dependency, memoryBasedUrl);
  }

  @Override
  public @NotNull URL loadModuleDependencyByRepository(
    @NotNull ModuleConfiguration configuration,
    @NotNull ModuleDependency dependency,
    @NotNull String repositoryUrl
  ) throws Exception {
    URL memoryBasedUrl = super.loadModuleDependencyByRepository(configuration, dependency, repositoryUrl);
    return this.loadDependency(dependency, memoryBasedUrl);
  }

  protected @NotNull URL loadDependency(@NotNull ModuleDependency dependency, @NotNull URL url) throws Exception {
    Path destFile = FileUtils.resolve(this.baseDirectory, dependency.getGroup().split("\\."))
      .resolve(dependency.getName())
      .resolve(dependency.getVersion())
      .resolve(String.format(FILE_NAME_FORMAT, dependency.getName(), dependency.getVersion()));
    FileUtils.ensureChild(this.baseDirectory, destFile);

    if (Files.notExists(destFile)) {
      Files.createDirectories(destFile.getParent());

      HttpURLConnection urlConnection = HttpConnectionProvider.provideConnection(url);
      urlConnection.setUseCaches(false);
      urlConnection.connect();

      try (InputStream inputStream = urlConnection.getInputStream()) {
        Files.copy(inputStream, destFile);
      }
    }

    return destFile.toUri().toURL();
  }

  public Path getBaseDirectory() {
    return this.baseDirectory;
  }
}
