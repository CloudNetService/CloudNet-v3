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

package eu.cloudnetservice.cloudnet.driver.network;

import com.google.common.base.Verify;
import com.google.common.base.VerifyException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import lombok.NonNull;
import org.jetbrains.annotations.Contract;

/**
 * Represents an immutable host and port mapping. Validation of a host and port is up to the caller. A host of this
 * class might be an ipv4/ipv6 address, but can also be the path to a unix domain socket.
 *
 * @since 4.0
 */
public record HostAndPort(@NonNull String host, int port) {

  private static final int NO_PORT = -1;

  // TODO: remove
  public HostAndPort(@NonNull InetSocketAddress socketAddress) {
    this(socketAddress.getAddress().getHostAddress(), socketAddress.getPort());
  }

  /**
   * Constructs a new host and port instance, validating the port.
   *
   * @param host the host of the address.
   * @param port the port of the address, or -1 if no port is given.
   * @throws NullPointerException if the given host is null.
   * @throws VerifyException      if the given port exceeds the port range.
   */
  public HostAndPort {
    Verify.verify(this.port() >= -1 && this.port() <= 65535, "invalid port given");
  }

  /**
   * Tries to convert the given socket address into a host and port, throwing an exception if not possible.
   *
   * @param socketAddress the socket address to convert.
   * @return the created host and port based on the given address.
   * @throws NullPointerException          if the given socket address is null.
   * @throws UnsupportedOperationException if the given socket address type cannot be converted.
   */
  @Contract("_ -> new")
  public static @NonNull HostAndPort fromSocketAddress(@NonNull SocketAddress socketAddress) {
    // TODO: better
    if (socketAddress instanceof InetSocketAddress inetSocketAddress) {
      return new HostAndPort(inetSocketAddress);
    }

    throw new UnsupportedOperationException("socketAddress must be instance of InetSocketAddress!");
  }

  /**
   * Checks if this host and port has a port provided. No port might be provided if the connection comes for example
   * from a unix domain socket.
   *
   * @return true if this host and port has a port given, false otherwise.
   */
  public boolean validPort() {
    return this.port != NO_PORT;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String toString() {
    return this.host + ":" + this.port;
  }
}
