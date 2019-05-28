package de.dytanic.cloudnet.network.listener;

import com.google.gson.reflect.TypeToken;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.cluster.IClusterNodeServer;
import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import de.dytanic.cloudnet.driver.network.def.PacketConstants;
import de.dytanic.cloudnet.driver.network.def.packet.PacketClientAuthorization;
import de.dytanic.cloudnet.driver.network.def.packet.PacketClientServerServiceInfoPublisher;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.permission.DefaultJsonFilePermissionManagement;
import de.dytanic.cloudnet.driver.service.ServiceId;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.event.cluster.NetworkChannelAuthClusterNodeSuccessEvent;
import de.dytanic.cloudnet.event.network.NetworkChannelAuthCloudServiceSuccessEvent;
import de.dytanic.cloudnet.network.packet.PacketServerAuthorizationResponse;
import de.dytanic.cloudnet.network.packet.PacketServerDeployLocalTemplate;
import de.dytanic.cloudnet.network.packet.PacketServerSetDatabaseGroupFilePermissions;
import de.dytanic.cloudnet.network.packet.PacketServerSetGlobalServiceInfoList;
import de.dytanic.cloudnet.network.packet.PacketServerSetGroupConfigurationList;
import de.dytanic.cloudnet.network.packet.PacketServerSetJsonFilePermissions;
import de.dytanic.cloudnet.network.packet.PacketServerSetServiceTaskList;
import de.dytanic.cloudnet.permission.DefaultDatabasePermissionManagement;
import de.dytanic.cloudnet.service.ICloudService;
import de.dytanic.cloudnet.template.ITemplateStorage;
import de.dytanic.cloudnet.template.LocalTemplateStorage;
import java.util.UUID;

public final class PacketClientAuthorizationListener implements
    IPacketListener {

  @Override
  public void handle(INetworkChannel channel, IPacket packet) throws Exception {
    if (packet.getHeader().contains("authorization") && packet.getHeader()
        .contains("credentials")) {
      JsonDocument credentials = packet.getHeader().getDocument("credentials");

      switch (packet.getHeader().get("authorization",
          PacketClientAuthorization.PacketAuthorizationType.class)) {
        case NODE_TO_NODE:
          if (credentials.contains("clusterId") && credentials
              .contains("clusterNode") &&
              getCloudNet().getConfig().getClusterConfig().getClusterId()
                  .equals(credentials.get("clusterId", UUID.class))) {
            NetworkClusterNode clusterNode = credentials
                .get("clusterNode", new TypeToken<NetworkClusterNode>() {
                }.getType());

            for (IClusterNodeServer clusterNodeServer : getCloudNet()
                .getClusterNodeServerProvider().getNodeServers()) {
              if (clusterNodeServer
                  .isAcceptableConnection(channel, clusterNode.getUniqueId())) {
                //- packet channel registry
                channel.getPacketRegistry()
                    .addListener(PacketConstants.INTERNAL_EVENTBUS_CHANNEL,
                        new PacketServerChannelMessageNodeListener());
                channel.getPacketRegistry()
                    .addListener(PacketConstants.INTERNAL_EVENTBUS_CHANNEL,
                        new PacketServerServiceInfoPublisherListener());
                channel.getPacketRegistry()
                    .addListener(PacketConstants.INTERNAL_EVENTBUS_CHANNEL,
                        new PacketServerUpdatePermissionsListener());
                //*= ------------------------------------
                channel.getPacketRegistry()
                    .addListener(PacketConstants.INTERNAL_CLUSTER_CHANNEL,
                        new PacketServerSetGlobalServiceInfoListListener());
                channel.getPacketRegistry()
                    .addListener(PacketConstants.INTERNAL_CLUSTER_CHANNEL,
                        new PacketServerSetGroupConfigurationListListener());
                channel.getPacketRegistry()
                    .addListener(PacketConstants.INTERNAL_CLUSTER_CHANNEL,
                        new PacketServerSetServiceTaskListListener());
                channel.getPacketRegistry()
                    .addListener(PacketConstants.INTERNAL_CLUSTER_CHANNEL,
                        new PacketServerSetJsonFilePermissionsListener());
                channel.getPacketRegistry()
                    .addListener(PacketConstants.INTERNAL_CLUSTER_CHANNEL,
                        new PacketServerSetDatabaseGroupFilePermissionsListener());
                channel.getPacketRegistry()
                    .addListener(PacketConstants.INTERNAL_CLUSTER_CHANNEL,
                        new PacketServerDeployLocalTemplateListener());
                channel.getPacketRegistry()
                    .addListener(PacketConstants.INTERNAL_CLUSTER_CHANNEL,
                        new PacketServerClusterNodeInfoUpdateListener());
                channel.getPacketRegistry()
                    .addListener(PacketConstants.INTERNAL_CLUSTER_CHANNEL,
                        new PacketServerConsoleLogEntryReceiveListener());
                //
                channel.getPacketRegistry().addListener(
                    PacketConstants.INTERNAL_PACKET_CLUSTER_MESSAGE_CHANNEL,
                    new PacketServerClusterChannelMessageListener());

                channel.getPacketRegistry()
                    .addListener(PacketConstants.INTERNAL_CALLABLE_CHANNEL,
                        new PacketClientCallablePacketReceiveListener());
                channel.getPacketRegistry()
                    .addListener(PacketConstants.INTERNAL_CALLABLE_CHANNEL,
                        new PacketClientSyncAPIPacketListener());
                channel.getPacketRegistry()
                    .addListener(PacketConstants.INTERNAL_CALLABLE_CHANNEL,
                        new PacketClusterSyncAPIPacketListener());

                channel.getPacketRegistry().addListener(
                    PacketConstants.INTERNAL_H2_DATABASE_UPDATE_MODULE,
                    new PacketServerH2DatabaseListener());
                channel.getPacketRegistry().addListener(
                    PacketConstants.INTERNAL_H2_DATABASE_UPDATE_MODULE,
                    new PacketServerSetH2DatabaseDataListener());
                //-

                channel.sendPacket(
                    new PacketServerAuthorizationResponse(true, "successful"));

                clusterNodeServer.setChannel(channel);
                CloudNetDriver.getInstance().getEventManager().callEvent(
                    new NetworkChannelAuthClusterNodeSuccessEvent(
                        clusterNodeServer, channel));

                getCloudNet().getLogger().info(
                    LanguageManager
                        .getMessage("cluster-server-networking-connected")
                        .replace("%id%", clusterNode.getUniqueId() + "")
                        .replace("%serverAddress%",
                            channel.getServerAddress().getHost() + ":" + channel
                                .getServerAddress().getPort())
                        .replace("%clientAddress%",
                            channel.getClientAddress().getHost() + ":" + channel
                                .getClientAddress().getPort())
                );

                this.sendSetupInformationPackets(channel,
                    credentials.contains("secondNodeConnection") && credentials
                        .getBoolean("secondNodeConnection"));
                return;
              }
            }
          }
          break;
        case WRAPPER_TO_NODE:
          if (credentials.contains("connectionKey") && credentials
              .contains("serviceId")) {
            String connectionKey = credentials.getString("connectionKey");
            ServiceId serviceId = credentials.get("serviceId", ServiceId.class);

            ICloudService cloudService = getCloudNet().getCloudServiceManager()
                .getCloudService(serviceId.getUniqueId());

            if (connectionKey != null && cloudService != null && cloudService
                .getConnectionKey().equals(connectionKey) &&
                cloudService.getServiceId().getTaskServiceId() == serviceId
                    .getTaskServiceId() &&
                cloudService.getServiceId().getNodeUniqueId()
                    .equals(serviceId.getNodeUniqueId())) {
              //- packet channel registry
              channel.getPacketRegistry()
                  .addListener(PacketConstants.INTERNAL_EVENTBUS_CHANNEL,
                      new PacketServerChannelMessageWrapperListener());
              //*= ------------------------------------
              channel.getPacketRegistry().addListener(
                  PacketConstants.INTERNAL_WRAPPER_TO_NODE_INFO_CHANNEL,
                  new PacketClientServiceInfoUpdateListener());

              channel.getPacketRegistry()
                  .addListener(PacketConstants.INTERNAL_CALLABLE_CHANNEL,
                      new PacketClientCallablePacketReceiveListener());
              channel.getPacketRegistry()
                  .addListener(PacketConstants.INTERNAL_CALLABLE_CHANNEL,
                      new PacketClientSyncAPIPacketListener());
              //-

              channel.sendPacket(
                  new PacketServerAuthorizationResponse(true, "successful"));

              cloudService.setNetworkChannel(channel);
              cloudService.getServiceInfoSnapshot().setConnected(true);

              CloudNetDriver.getInstance().getEventManager().callEvent(
                  new NetworkChannelAuthCloudServiceSuccessEvent(cloudService,
                      channel));

              getCloudNet().getLogger().info(LanguageManager
                  .getMessage("cloud-service-networking-connected")
                  .replace("%id%",
                      cloudService.getServiceId().getUniqueId().toString() + "")
                  .replace("%task%",
                      cloudService.getServiceId().getTaskName() + "")
                  .replace("%serverAddress%",
                      channel.getServerAddress().getHost() + ":" + channel
                          .getServerAddress().getPort())
                  .replace("%clientAddress%",
                      channel.getClientAddress().getHost() + ":" + channel
                          .getClientAddress().getPort())
              );

              getCloudNet().getNetworkClient().sendPacket(
                  new PacketClientServerServiceInfoPublisher(
                      cloudService.getServiceInfoSnapshot(),
                      PacketClientServerServiceInfoPublisher.PublisherType.CONNECTED));
              getCloudNet().getNetworkServer().sendPacket(
                  new PacketClientServerServiceInfoPublisher(
                      cloudService.getServiceInfoSnapshot(),
                      PacketClientServerServiceInfoPublisher.PublisherType.CONNECTED));
              return;
            }
          }
          break;
      }

      channel.sendPacket(
          new PacketServerAuthorizationResponse(false, "access denied"));
      channel.close();
    }
  }

  private void sendSetupInformationPackets(INetworkChannel channel,
      boolean secondNodeConnection) {
    channel.sendPacket(new PacketServerSetGlobalServiceInfoList(
        getCloudNet().getCloudServiceManager().getGlobalServiceInfoSnapshots()
            .values()));

    if (!secondNodeConnection) {
      channel.sendPacket(new PacketServerSetGroupConfigurationList(
          getCloudNet().getGroupConfigurations()));
      channel.sendPacket(new PacketServerSetServiceTaskList(
          getCloudNet().getPermanentServiceTasks()));

      if (getCloudNet()
          .getPermissionManagement() instanceof DefaultJsonFilePermissionManagement) {
        channel.sendPacket(new PacketServerSetJsonFilePermissions(
            getCloudNet().getPermissionManagement().getUsers(),
            getCloudNet().getPermissionManagement().getGroups()
        ));
      }

      if (getCloudNet()
          .getPermissionManagement() instanceof DefaultDatabasePermissionManagement) {
        channel.sendPacket(new PacketServerSetDatabaseGroupFilePermissions(
            getCloudNet().getPermissionManagement().getGroups()
        ));
      }

      ITemplateStorage templateStorage = CloudNetDriver.getInstance()
          .getServicesRegistry().getService(ITemplateStorage.class,
              LocalTemplateStorage.LOCAL_TEMPLATE_STORAGE);

      byte[] bytes;
      for (ServiceTemplate serviceTemplate : templateStorage.getTemplates()) {
        bytes = templateStorage.toZipByteArray(serviceTemplate);
        channel.sendPacket(
            new PacketServerDeployLocalTemplate(serviceTemplate, bytes));
      }

      CloudNet.getInstance().publishH2DatabaseDataToCluster(channel);
      CloudNet.getInstance().publishH2DatabaseDataToCluster(channel);
    }
  }

  private CloudNet getCloudNet() {
    return CloudNet.getInstance();
  }
}