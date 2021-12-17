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

package de.dytanic.cloudnet.event.service;

import de.dytanic.cloudnet.driver.event.ICancelable;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import de.dytanic.cloudnet.service.ICloudService;
import org.jetbrains.annotations.NotNull;

public final class CloudServicePreLifecycleEvent extends CloudServiceEvent implements ICancelable {

  private final ServiceLifeCycle targetLifecycle;
  private volatile boolean cancelled;

  public CloudServicePreLifecycleEvent(@NotNull ICloudService service, @NotNull ServiceLifeCycle targetLifecycle) {
    super(service);
    this.targetLifecycle = targetLifecycle;
  }

  public @NotNull ServiceLifeCycle getTargetLifecycle() {
    return this.targetLifecycle;
  }

  @Override
  public boolean cancelled() {
    return this.cancelled;
  }

  @Override
  public void cancelled(boolean value) {
    this.cancelled = value;
  }
}
