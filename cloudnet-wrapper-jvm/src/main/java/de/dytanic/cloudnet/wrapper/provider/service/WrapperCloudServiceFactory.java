package de.dytanic.cloudnet.wrapper.provider.service;

import com.google.common.base.Preconditions;
import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.provider.service.CloudServiceFactory;
import de.dytanic.cloudnet.driver.service.*;
import de.dytanic.cloudnet.wrapper.Wrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class WrapperCloudServiceFactory implements CloudServiceFactory {

    private final Wrapper wrapper;

    public WrapperCloudServiceFactory(Wrapper wrapper) {
        this.wrapper = wrapper;
    }

    @Nullable
    @Override
    public ServiceInfoSnapshot createCloudService(ServiceTask serviceTask) {
        Preconditions.checkNotNull(serviceTask);

        try {
            return this.createCloudServiceAsync(serviceTask).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    @Override
    public @Nullable ServiceInfoSnapshot createCloudService(ServiceTask serviceTask, int taskId) {
        Preconditions.checkNotNull(serviceTask);

        try {
            return this.createCloudServiceAsync(serviceTask, taskId).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    @Nullable
    @Override
    public ServiceInfoSnapshot createCloudService(ServiceConfiguration serviceConfiguration) {
        Preconditions.checkNotNull(serviceConfiguration);

        try {
            return this.createCloudServiceAsync(serviceConfiguration).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    @Nullable
    @Override
    public ServiceInfoSnapshot createCloudService(String name,
                                                  String runtime,
                                                  boolean autoDeleteOnStop,
                                                  boolean staticService,
                                                  Collection<ServiceRemoteInclusion> includes,
                                                  Collection<ServiceTemplate> templates,
                                                  Collection<ServiceDeployment> deployments,
                                                  Collection<String> groups,
                                                  ProcessConfiguration processConfiguration,
                                                  JsonDocument properties,
                                                  Integer port) {
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(includes);
        Preconditions.checkNotNull(templates);
        Preconditions.checkNotNull(deployments);
        Preconditions.checkNotNull(groups);
        Preconditions.checkNotNull(processConfiguration);

        try {
            return this.createCloudServiceAsync(name, runtime, autoDeleteOnStop, staticService, includes, templates, deployments, groups, processConfiguration, properties, port).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    @Nullable
    @Override
    public Collection<ServiceInfoSnapshot> createCloudService(String nodeUniqueId,
                                                              int amount,
                                                              String name,
                                                              String runtime,
                                                              boolean autoDeleteOnStop,
                                                              boolean staticService,
                                                              Collection<ServiceRemoteInclusion> includes,
                                                              Collection<ServiceTemplate> templates,
                                                              Collection<ServiceDeployment> deployments,
                                                              Collection<String> groups,
                                                              ProcessConfiguration processConfiguration,
                                                              JsonDocument properties,
                                                              Integer port) {
        Preconditions.checkNotNull(nodeUniqueId);
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(includes);
        Preconditions.checkNotNull(templates);
        Preconditions.checkNotNull(deployments);
        Preconditions.checkNotNull(groups);
        Preconditions.checkNotNull(processConfiguration);

        try {
            return this.createCloudServiceAsync(nodeUniqueId, amount, name, runtime, autoDeleteOnStop, staticService, includes, templates, deployments, groups, processConfiguration, properties, port).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    @Override
    @NotNull
    public ITask<ServiceInfoSnapshot> createCloudServiceAsync(ServiceTask serviceTask) {
        Preconditions.checkNotNull(serviceTask);

        return this.wrapper.getPacketQueryProvider().sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "create_CloudService_by_serviceTask").append("serviceTask", serviceTask), null,
                documentPair -> documentPair.getFirst().get("serviceInfoSnapshot", new TypeToken<ServiceInfoSnapshot>() {
                }.getType()));
    }

    @Override
    public @NotNull ITask<ServiceInfoSnapshot> createCloudServiceAsync(ServiceTask serviceTask, int taskId) {
        Preconditions.checkNotNull(serviceTask);

        return this.wrapper.getPacketQueryProvider().sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "create_CloudService_by_serviceTask").append("serviceTask", serviceTask).append("taskId", taskId), null,
                documentPair -> documentPair.getFirst().get("serviceInfoSnapshot", new TypeToken<ServiceInfoSnapshot>() {
                }.getType()));
    }

    @Override
    @NotNull
    public ITask<ServiceInfoSnapshot> createCloudServiceAsync(ServiceConfiguration serviceConfiguration) {
        Preconditions.checkNotNull(serviceConfiguration);

        return this.wrapper.getPacketQueryProvider().sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "create_CloudService_by_serviceConfiguration").append("serviceConfiguration", serviceConfiguration), null,
                documentPair -> documentPair.getFirst().get("serviceInfoSnapshot", new TypeToken<ServiceInfoSnapshot>() {
                }.getType()));
    }

    @Override
    @NotNull
    public ITask<ServiceInfoSnapshot> createCloudServiceAsync(String name,
                                                              String runtime,
                                                              boolean autoDeleteOnStop,
                                                              boolean staticService,
                                                              Collection<ServiceRemoteInclusion> includes,
                                                              Collection<ServiceTemplate> templates,
                                                              Collection<ServiceDeployment> deployments,
                                                              Collection<String> groups,
                                                              ProcessConfiguration processConfiguration,
                                                              JsonDocument properties,
                                                              Integer port) {
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(includes);
        Preconditions.checkNotNull(templates);
        Preconditions.checkNotNull(deployments);
        Preconditions.checkNotNull(groups);
        Preconditions.checkNotNull(processConfiguration);

        return this.wrapper.getPacketQueryProvider().sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "create_cloud_service_custom")
                        .append("name", name)
                        .append("runtime", runtime)
                        .append("autoDeleteOnStop", autoDeleteOnStop)
                        .append("staticService", staticService)
                        .append("includes", includes)
                        .append("templates", templates)
                        .append("deployments", deployments)
                        .append("groups", groups)
                        .append("processConfiguration", processConfiguration)
                        .append("properties", properties)
                        .append("port", port),
                null,
                documentPair -> documentPair.getFirst().get("serviceInfoSnapshot", new TypeToken<ServiceInfoSnapshot>() {
                }.getType()));
    }

    @Override
    @NotNull
    public ITask<Collection<ServiceInfoSnapshot>> createCloudServiceAsync(String nodeUniqueId,
                                                                          int amount,
                                                                          String name,
                                                                          String runtime,
                                                                          boolean autoDeleteOnStop,
                                                                          boolean staticService,
                                                                          Collection<ServiceRemoteInclusion> includes,
                                                                          Collection<ServiceTemplate> templates,
                                                                          Collection<ServiceDeployment> deployments,
                                                                          Collection<String> groups,
                                                                          ProcessConfiguration processConfiguration,
                                                                          JsonDocument properties,
                                                                          Integer port) {
        Preconditions.checkNotNull(nodeUniqueId);
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(includes);
        Preconditions.checkNotNull(templates);
        Preconditions.checkNotNull(deployments);
        Preconditions.checkNotNull(groups);
        Preconditions.checkNotNull(processConfiguration);

        return this.wrapper.getPacketQueryProvider().sendCallablePacketWithAsDriverSyncAPIWithNetworkConnector(
                new JsonDocument(PacketConstants.SYNC_PACKET_ID_PROPERTY, "create_cloud_service_custom_selected_node_and_amount")
                        .append("nodeUniqueId", nodeUniqueId)
                        .append("amount", amount)
                        .append("name", name)
                        .append("runtime", runtime)
                        .append("autoDeleteOnStop", autoDeleteOnStop)
                        .append("staticService", staticService)
                        .append("includes", includes)
                        .append("templates", templates)
                        .append("deployments", deployments)
                        .append("groups", groups)
                        .append("processConfiguration", processConfiguration)
                        .append("properties", properties)
                        .append("port", port),
                null,
                documentPair -> documentPair.getFirst().get("serviceInfoSnapshots", new TypeToken<Collection<ServiceInfoSnapshot>>() {
                }.getType()));
    }
}
