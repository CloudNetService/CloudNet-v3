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

package eu.cloudnetservice.cloudnet.driver.provider;

import eu.cloudnetservice.cloudnet.common.concurrent.CompletableTask;
import eu.cloudnetservice.cloudnet.common.concurrent.Task;
import eu.cloudnetservice.cloudnet.driver.channel.ChannelMessageSender;
import eu.cloudnetservice.cloudnet.driver.network.rpc.annotation.RPCValidation;
import eu.cloudnetservice.cloudnet.driver.service.ServiceDeployment;
import eu.cloudnetservice.cloudnet.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.cloudnet.driver.service.ServiceLifeCycle;
import eu.cloudnetservice.cloudnet.driver.service.ServiceRemoteInclusion;
import eu.cloudnetservice.cloudnet.driver.service.ServiceTemplate;
import java.util.Queue;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * An instance of this class represents a manageable service in the cluster which is stateless. This is the main
 * difference to a service snapshot. While the snapshot holds the service information in a specific moment, the provider
 * of the service will always be ready to execute actions on a service (unless the provider is no longer valid).
 * <p>
 * The provider for a service could possibly target other services which were created with the same name or unique id
 * this provider was created for. Therefore, acquiring a new provider for a service when you're unsure if the old
 * service still exists is recommended.
 *
 * @since 4.0
 */
@RPCValidation
public interface SpecificCloudServiceProvider {

  /**
   * Get the last reported service info snapshot of the service. This snapshot is updated on an event basis, therefore
   * this method will not always return a snapshot which is brand-new. More specifically, for example the bridge will
   * cause a service info update each time a player joins/leaves the current server. This leads to unexpected
   * information when for example querying the process snapshot or the creation time, as these data can be very old.
   * <p>
   * This information is null when either the underlying service of this provider was unregistered from the system or
   * when creating the provider the service didn't yet exist.
   * <p>
   * If you need an update-to-date version of the snapshot, use {@link #forceUpdateServiceInfo()} instead.
   *
   * @return the last reported information snapshot of the service, can be null as described above.
   */
  @Nullable ServiceInfoSnapshot serviceInfo();

  /**
   * Get if this provider is still valid. A provider which is valid
   * <ol>
   *   <li>targets a service which still exists.
   *   <li>targets a service which is not marked as deleted.
   * </ol>
   *
   * @return true if this provider is still valid, false otherwise.
   */
  boolean valid();

  /**
   * Forces the service to update its service info and always returns a newly created snapshot of the service (other
   * than returning the last reported snapshot what {@link #serviceInfo()} does). This method returns null when the
   * underlying service
   * <ol>
   *   <li>doesn't exist anymore.
   *   <li>is not started and therefore not connected to a node.
   * </ol>
   *
   * @return a newly created service snapshot, can be null as described above.
   */
  @Nullable ServiceInfoSnapshot forceUpdateServiceInfo();

  /**
   * Adds the given service template to the inclusion queue. This does not mean that the given template will be copied
   * directly onto the service. The template will be copied when
   * <ol>
   *   <li>the service gets prepared, for example when restarting the service.
   *   <li>the templates get included via the associated method in this provider.
   * </ol>
   *
   * @param serviceTemplate the service template to enqueue.
   * @throws NullPointerException if the given service template is null.
   */
  void addServiceTemplate(@NonNull ServiceTemplate serviceTemplate);

  /**
   * Adds the given service remote inclusion to the queue. This does not mean that the inclusion gets copied directly
   * onto the service. The inclusion will be included when:
   * <ol>
   *   <li>the service gets prepared, for example when restarting the service.
   *   <li>the inclusions get included via the associated methods in this provider.
   * </ol>
   *
   * @param serviceRemoteInclusion the inclusion to enqueue.
   * @throws NullPointerException if the given inclusion is null.
   */
  void addServiceRemoteInclusion(@NonNull ServiceRemoteInclusion serviceRemoteInclusion);

  /**
   * Adds the given service deployment to the queue. This does not mean that the deployment gets executed directly. It
   * wil be executed when:
   * <ol>
   *   <li>the service stops, for example when deleting it.
   *   <li>the deployments get executed via the associated methods in this provider.
   * </ol>
   *
   * @param serviceDeployment the deployment to enqueue.
   * @throws NullPointerException if the given deployment is null.
   */
  void addServiceDeployment(@NonNull ServiceDeployment serviceDeployment);

  /**
   * Get all log messages which are currently cached on the node this service is running on. Modifications to the
   * returned queue might be possible. The size of the returned collection is always not bigger than configured in the
   * node configuration the associated service is running on.
   * <p>
   * This method never return null but can return an empty queue if the underlying service does not exist.
   *
   * @return all cached log messages of the service on the node the service is running on.
   */
  @NonNull Queue<String> cachedLogMessages();

  /**
   * Enables or disabled the screen event handling. When the log events get enabled an event will be called on the given
   * sender of the request holding information about the log line. The provided channel represents the event channel to
   * which the listener need to listen in order to receive the events, set this to {@code *} to call all event
   * listeners.
   *
   * @param channelMessageSender the sender who should receive the log events.
   * @param channel              the event channel to call the log entry event in.
   * @return true if the log events were enabled for the sender, false if they got disabled.
   * @throws NullPointerException if either the given message sender or channel is null.
   */
  boolean toggleScreenEvents(@NonNull ChannelMessageSender channelMessageSender, @NonNull String channel);

  /**
   * Sets the service lifecycle to stopped and executes the appropriate actions to change to the stopped state.
   */
  default void stop() {
    this.updateLifecycle(ServiceLifeCycle.STOPPED);
  }

  /**
   * Sets the service lifecycle to started and executes the appropriate actions to change to the started state.
   */
  default void start() {
    this.updateLifecycle(ServiceLifeCycle.RUNNING);
  }

  /**
   * Sets the service lifecycle to deleted and executes the appropriate actions to change to the deleted state.
   */
  default void delete() {
    this.updateLifecycle(ServiceLifeCycle.DELETED);
  }

  /**
   * Stops this service and then tries to start it again. Note that this method will stop and delete the service, but
   * not start the service again when auto delete on stop is active for the service.
   */
  void restart();

  /**
   * Requests a change of the service lifecycle to the given one. This method has no effect if to the given lifecycle
   * cannot be switched from the current lifecycle of the service.
   *
   * @param lifeCycle the service lifecycle to switch to.
   * @throws NullPointerException if the given lifecycle is null.
   */
  void updateLifecycle(@NonNull ServiceLifeCycle lifeCycle);

  /**
   * Stops the service if it is currently running marks it as deleted. Other than the delete method, in this case all
   * files associated with the service will get deleted permanently even if the service is static. If you just want to
   * stop and delete all files of the service when it is non-static use {@link #delete()} instead.
   * <p>
   * Deployments added to the service will get executed before the files get deleted.
   */
  void deleteFiles();

  /**
   * Executes the given command on the service if it is running. The given command line will be sent to stdin directly.
   *
   * @param command the command line to execute.
   * @throws NullPointerException if the given command line is null.
   */
  void runCommand(@NonNull String command);

  /**
   * Copies all queued templates onto the service without further checks. Note that this can lead to errors if you try
   * to override locked files or files which are in use (for example the application jar file).
   */
  void includeWaitingServiceTemplates();

  /**
   * Downloads and copies all waiting inclusions onto the service without further checks. Note that this can lead to
   * errors if you try to override locked files or files which are in use (for example the application jar file).
   */
  void includeWaitingServiceInclusions();

  /**
   * Executes all deployments which were previously added to the associated service and optionally removes them once
   * they were executed successfully.
   *
   * @param removeDeployments if the deployments should get removed after executing them.
   */
  void deployResources(boolean removeDeployments);

  /**
   * Executes all deployments which were previously added to the associated service and removes them once they were
   * executed. This method call is identical to {@code provider.deployResources(true)}.
   */
  default void removeAndExecuteDeployments() {
    this.deployResources(true);
  }

  /**
   * Get the last reported service info snapshot of the service. This snapshot is updated on an event basis, therefore
   * this method will not always return a snapshot which is brand-new. More specifically, for example the bridge will
   * cause a service info update each time a player joins/leaves the current server. This leads to unexpected
   * information when for example querying the process snapshot or the creation time, as these data can be very old.
   * <p>
   * This information is null when either the underlying service of this provider was unregistered from the system or
   * when creating the provider the service didn't yet exist.
   * <p>
   * If you need an update-to-date version of the snapshot, use {@link #forceUpdateServiceInfo()} instead.
   *
   * @return a task completed with the last reported snapshot of the service, can be null as described above.
   */
  default @NonNull Task<ServiceInfoSnapshot> serviceInfoAsync() {
    return CompletableTask.supply(this::serviceInfo);
  }

  /**
   * Get if this provider is still valid. A provider which is valid
   * <ol>
   *   <li>targets a service which still exists.
   *   <li>targets a service which is not marked as deleted.
   * </ol>
   *
   * @return a task completed with true if this provider is still valid, false otherwise.
   */
  default @NonNull Task<Boolean> validAsync() {
    return CompletableTask.supply(this::valid);
  }

  /**
   * Forces the service to update its service info and always returns a newly created snapshot of the service (other
   * than returning the last reported snapshot what {@link #serviceInfo()} does). This method returns null when the
   * underlying service
   * <ol>
   *   <li>doesn't exist anymore.
   *   <li>is not started and therefore not connected to a node.
   * </ol>
   *
   * @return a task completed with a newly created service snapshot, can be null as described above.
   */
  default @NonNull Task<ServiceInfoSnapshot> forceUpdateServiceInfoAsync() {
    return CompletableTask.supply(this::forceUpdateServiceInfo);
  }

  /**
   * Adds the given service template to the inclusion queue. This does not mean that the given template will be copied
   * directly onto the service. The template will be copied when
   * <ol>
   *   <li>the service gets prepared, for example when restarting the service.
   *   <li>the templates get included via the associated method in this provider.
   * </ol>
   *
   * @param serviceTemplate the service template to enqueue.
   * @return a task completed when the given service template was enqueued.
   * @throws NullPointerException if the given service template is null.
   */
  default @NonNull Task<Void> addServiceTemplateAsync(@NonNull ServiceTemplate serviceTemplate) {
    return CompletableTask.supply(() -> this.addServiceTemplate(serviceTemplate));
  }

  /**
   * Adds the given service remote inclusion to the queue. This does not mean that the inclusion gets copied directly
   * onto the service. The inclusion will be included when:
   * <ol>
   *   <li>the service gets prepared, for example when restarting the service.
   *   <li>the inclusions get included via the associated methods in this provider.
   * </ol>
   *
   * @param serviceRemoteInclusion the inclusion to enqueue.
   * @return a task completed when the given service remote inclusion was enqueued.
   * @throws NullPointerException if the given inclusion is null.
   */
  default @NonNull Task<Void> addServiceRemoteInclusionAsync(@NonNull ServiceRemoteInclusion serviceRemoteInclusion) {
    return CompletableTask.supply(() -> this.addServiceRemoteInclusion(serviceRemoteInclusion));
  }

  /**
   * Adds the given service deployment to the queue. This does not mean that the deployment gets executed directly. It
   * wil be executed when:
   * <ol>
   *   <li>the service stops, for example when deleting it.
   *   <li>the deployments get executed via the associated methods in this provider.
   * </ol>
   *
   * @param serviceDeployment the deployment to enqueue.
   * @return a task completed when the given service deployment was enqueued.
   * @throws NullPointerException if the given deployment is null.
   */
  default @NonNull Task<Void> addServiceDeploymentAsync(@NonNull ServiceDeployment serviceDeployment) {
    return CompletableTask.supply(() -> this.addServiceDeployment(serviceDeployment));
  }

  /**
   * Get all log messages which are currently cached on the node this service is running on. Modifications to the
   * returned queue might be possible. The size of the returned collection is always not bigger than configured in the
   * node configuration the associated service is running on.
   * <p>
   * This method never return null but can return an empty queue if the underlying service does not exist.
   *
   * @return a task completed with all cached service log messages on the node the service is running on.
   */
  default @NonNull Task<Queue<String>> cachedLogMessagesAsync() {
    return CompletableTask.supply(this::cachedLogMessages);
  }

  /**
   * Enables or disabled the screen event handling. When the log events get enabled an event will be called on the given
   * sender of the request holding information about the log line. The provided channel represents the event channel to
   * which the listener need to listen in order to receive the events, set this to {@code *} to call all event
   * listeners.
   *
   * @param sender the sender who should receive the log events.
   * @param chan   the event channel to call the log entry event in.
   * @return a task completed with true if the log events were enabled for the sender, false if they got disabled.
   * @throws NullPointerException if either the given message sender or channel is null.
   */
  default @NonNull Task<Boolean> toggleScreenEventsAsync(@NonNull ChannelMessageSender sender, @NonNull String chan) {
    return CompletableTask.supply(() -> this.toggleScreenEvents(sender, chan));
  }

  /**
   * Sets the service lifecycle to stopped and executes the appropriate actions to change to the stopped state.
   *
   * @return a task completed when the service lifecycle changed to stopped.
   */
  default @NonNull Task<Void> stopAsync() {
    return this.updateLifecycleAsync(ServiceLifeCycle.STOPPED);
  }

  /**
   * Sets the service lifecycle to started and executes the appropriate actions to change to the started state.
   *
   * @return a task completed when the service lifecycle changed to running.
   */
  default @NonNull Task<Void> startAsync() {
    return this.updateLifecycleAsync(ServiceLifeCycle.RUNNING);
  }

  /**
   * Sets the service lifecycle to deleted and executes the appropriate actions to change to the deleted state.
   *
   * @return a task completed when the service state changed to deleted.
   */
  default @NonNull Task<Void> deleteAsync() {
    return this.updateLifecycleAsync(ServiceLifeCycle.DELETED);
  }

  /**
   * Stops this service and then tries to start it again. Note that this method will stop and delete the service, but
   * not start the service again when auto delete on stop is active for the service.
   *
   * @return a task completed when the service was restarted.
   */
  default @NonNull Task<Void> restartAsync() {
    return CompletableTask.supply(this::restart);
  }

  /**
   * Requests a change of the service lifecycle to the given one. This method has no effect if to the given lifecycle
   * cannot be switched from the current lifecycle of the service.
   *
   * @param lifeCycle the service lifecycle to switch to.
   * @return a task completed when the lifecycle change was tried.
   * @throws NullPointerException if the given lifecycle is null.
   */
  default @NonNull Task<Void> updateLifecycleAsync(@NonNull ServiceLifeCycle lifeCycle) {
    return CompletableTask.supply(() -> this.updateLifecycle(lifeCycle));
  }

  /**
   * Stops the service if it is currently running marks it as deleted. Other than the delete method, in this case all
   * files associated with the service will get deleted permanently even if the service is static. If you just want to
   * stop and delete all files of the service when it is non-static use {@link #delete()} instead.
   * <p>
   * Deployments added to the service will get executed before the files get deleted.
   *
   * @return a task completed when the service files were deleted.
   */
  default @NonNull Task<Void> deleteFilesAsync() {
    return CompletableTask.supply(this::deleteFiles);
  }

  /**
   * Executes the given command on the service if it is running. The given command line will be sent to stdin directly.
   *
   * @param command the command line to execute.
   * @return a task completed when the command was send to the service.
   * @throws NullPointerException if the given command line is null.
   */
  default @NonNull Task<Void> runCommandAsync(@NonNull String command) {
    return CompletableTask.supply(() -> this.runCommand(command));
  }

  /**
   * Copies all queued templates onto the service without further checks. Note that this can lead to errors if you try
   * to override locked files or files which are in use (for example the application jar file).
   *
   * @return a task completed when the waiting service templates were included.
   */
  default @NonNull Task<Void> includeWaitingServiceTemplatesAsync() {
    return CompletableTask.supply(this::includeWaitingServiceTemplates);
  }

  /**
   * Downloads and copies all waiting inclusions onto the service without further checks. Note that this can lead to
   * errors if you try to override locked files or files which are in use (for example the application jar file).
   *
   * @return a task completed when the waiting service inclusions were included.
   */
  default @NonNull Task<Void> includeWaitingServiceInclusionsAsync() {
    return CompletableTask.supply(this::includeWaitingServiceInclusions);
  }

  /**
   * Executes all deployments which were previously added to the associated service and optionally removes them once
   * they were executed successfully.
   *
   * @param removeDeployments if the deployments should get removed after executing them.
   * @return a task completed when all waiting service deployments were executed.
   */
  default @NonNull Task<Void> deployResourcesAsync(boolean removeDeployments) {
    return CompletableTask.supply(() -> this.deployResources(removeDeployments));
  }

  /**
   * Executes all deployments which were previously added to the associated service and removes them once they were
   * executed. This method call is identical to {@code provider.deployResourcesAsync(true)}.
   *
   * @return a task completed when all waiting service deployments were executed.
   */
  default @NonNull Task<Void> executeAndRemoveDeploymentsAsync() {
    return this.deployResourcesAsync(true);
  }
}
