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

package eu.cloudnetservice.cloudnet.node.network.packet;

import eu.cloudnetservice.cloudnet.driver.network.buffer.DataBuf;
import eu.cloudnetservice.cloudnet.driver.network.cluster.NetworkClusterNodeInfoSnapshot;
import eu.cloudnetservice.cloudnet.driver.network.def.NetworkConstants;
import eu.cloudnetservice.cloudnet.driver.network.protocol.BasePacket;
import lombok.NonNull;

public final class PacketServerServiceSyncAckPacket extends BasePacket {

  public PacketServerServiceSyncAckPacket(@NonNull NetworkClusterNodeInfoSnapshot info, @NonNull DataBuf localData) {
    super(
      NetworkConstants.INTERNAL_SERVICE_SYNC_ACK_CHANNEL,
      DataBuf.empty()
        .writeObject(info)
        .writeDataBuf(localData));
  }
}
