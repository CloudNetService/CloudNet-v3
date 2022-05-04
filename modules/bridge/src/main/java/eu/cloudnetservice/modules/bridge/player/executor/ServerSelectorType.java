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

package eu.cloudnetservice.modules.bridge.player.executor;

import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.modules.bridge.BridgeServiceProperties;
import java.util.Comparator;
import java.util.concurrent.ThreadLocalRandom;
import lombok.NonNull;

public enum ServerSelectorType {

  HIGHEST_PLAYERS(Comparator.comparingInt(o -> o.property(BridgeServiceProperties.ONLINE_COUNT).orElse(0))),
  LOWEST_PLAYERS((o1, o2) -> Integer.compare(
    o2.property(BridgeServiceProperties.ONLINE_COUNT).orElse(0),
    o1.property(BridgeServiceProperties.ONLINE_COUNT).orElse(0))),
  RANDOM(Comparator.comparingInt(value -> ThreadLocalRandom.current().nextInt(2) - 1));

  private final Comparator<ServiceInfoSnapshot> comparator;

  /**
   * Creates a new server selector type enum constant with the given service comparator.
   *
   * @param comparator the comparator to apply to the services.
   * @throws NullPointerException if the comparator is null.
   */
  ServerSelectorType(@NonNull Comparator<ServiceInfoSnapshot> comparator) {
    this.comparator = comparator;
  }

  /**
   * Gets the comparator used to sort the services infos according to the server selector type.
   *
   * @return the comparator for the server selector.
   */
  public @NonNull Comparator<ServiceInfoSnapshot> comparator() {
    return this.comparator;
  }
}
