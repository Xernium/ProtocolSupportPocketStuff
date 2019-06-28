package protocolsupportpocketstuff.resourcepacks;

import protocolsupport.api.Connection.PacketListener;
import protocolsupport.api.events.ConnectionHandshakeEvent;
import protocolsupport.protocol.ConnectionImpl;
import protocolsupport.protocol.serializer.MiscSerializer;
import protocolsupport.protocol.typeremapper.pe.PEPacketIDs;
import protocolsupportpocketstuff.ProtocolSupportPocketStuff;
import protocolsupportpocketstuff.api.PocketStuffAPI;
import protocolsupportpocketstuff.api.resourcepacks.ResourcePack;
import protocolsupportpocketstuff.api.util.PocketCon;
import protocolsupportpocketstuff.api.util.PocketPacketHandler;
import protocolsupportpocketstuff.api.util.PocketPacketListener;
import protocolsupportpocketstuff.packet.play.DisconnectPacket;
import protocolsupportpocketstuff.packet.play.ResourcePackChunkDataPacket;
import protocolsupportpocketstuff.packet.play.ResourcePackDataInfoPacket;
import protocolsupportpocketstuff.packet.play.ResourcePackResponsePacket;
import protocolsupportpocketstuff.packet.play.ResourcePackStackPacket;
import protocolsupportpocketstuff.packet.play.ResourcePackInfoPacket;
import protocolsupportpocketstuff.packet.play.ResourcePackRequestPacket;
import protocolsupportpocketstuff.util.packet.serializer.PacketSerializer;

import java.util.ArrayList;
import java.util.Optional;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import io.netty.buffer.ByteBuf;

public class ResourcePackListener extends PacketListener implements PocketPacketListener, Listener {

	private static final ProtocolSupportPocketStuff plugin = ProtocolSupportPocketStuff.getInstance();
	private static final String packFinished = "peResourcesDownloaded";

	@PocketPacketHandler
	public void onResourceResponse(ConnectionImpl connection, ResourcePackResponsePacket packet) {
		switch (packet.getStatus()) {
			case ResourcePackResponsePacket.REFUSED: {
				PocketCon.sendPocketPacket(connection, new DisconnectPacket(false, ChatColor.RED + "You must accept resource packs to join this server."));
				return;
			}
			case ResourcePackResponsePacket.SEND_PACKS: {
				plugin.debug("Sending client missing packs...");
				ArrayList<ResourcePack> packs = new ArrayList<ResourcePack>();
				PocketStuffAPI.getResourcePackManager().getBehaviorPacks().stream().filter(it -> packet.getMissingPacks().contains(it.getPackId())).forEach(pack -> packs.add(pack));
				PocketStuffAPI.getResourcePackManager().getResourcePacks().stream().filter(it -> packet.getMissingPacks().contains(it.getPackId())).forEach(pack -> packs.add(pack));
				for (ResourcePack pack : packs) {
					PocketCon.sendPocketPacket(connection, new ResourcePackDataInfoPacket(pack));
				}
				return;
			}
			case ResourcePackResponsePacket.HAVE_ALL_PACKS: {
				plugin.debug("Sending client resource stack packet...");
				connection.addMetadata(packFinished, true);
				ResourcePackStackPacket stackPacket = new ResourcePackStackPacket(PocketStuffAPI.getResourcePackManager().resourcePacksRequired(), 
						PocketStuffAPI.getResourcePackManager().getBehaviorPacks(), PocketStuffAPI.getResourcePackManager().getResourcePacks());
				PocketCon.sendPocketPacket(connection, stackPacket);
				return;
			}
			case ResourcePackResponsePacket.COMPLETED:
			default: {
				return;
			}
		}
	}
	
	@PocketPacketHandler
	public void onResourceRequest(ConnectionImpl connection, ResourcePackRequestPacket packet) {
		plugin.debug("Sending part " + packet.getChunkIndex() + " of the " + packet.getPackId() + " res/beh pack");
		ArrayList<ResourcePack> packs = new ArrayList<ResourcePack>();
		packs.addAll(PocketStuffAPI.getResourcePackManager().getBehaviorPacks());
		packs.addAll(PocketStuffAPI.getResourcePackManager().getResourcePacks());
		Optional<ResourcePack> pack = packs.stream().filter(it -> it.getPackId().equals(packet.getPackId())).findFirst();
		if (!pack.isPresent()) {
			plugin.debug("Client requested pack " + packet.getPackId() + ", however I don't have it!");
			PocketCon.sendPocketPacket(connection, new DisconnectPacket(false, ChatColor.RED + "Got a resource pack chunk request for unknown pack with UUID " + packet.getPackId()));
			return;
		}
		PocketCon.sendPocketPacket(connection, new ResourcePackChunkDataPacket(packet.getPackId(), packet.getChunkIndex(), pack.get().getPackChunk(packet.getChunkIndex())));
	}

	@EventHandler
	public void onConnectionHandshake(ConnectionHandshakeEvent event) {
		ConnectionImpl connection = (ConnectionImpl) event.getConnection();
		if (PocketCon.isPocketConnection(connection)) {
			connection.addPacketListener(new ResourcePacketReplacer(connection));
		}
	}

	public static class ResourcePacketReplacer extends PacketListener {

		protected ArrayList<ByteBuf> queue = new ArrayList<>(2000);
		protected final ConnectionImpl connection;
		protected boolean queuePackets = false;

		public ResourcePacketReplacer(ConnectionImpl connection) {
			this.connection = connection;
		}

		@Override
		public void onRawPacketSending(RawPacketEvent event) {
			super.onRawPacketSending(event);
			ByteBuf serverdata = event.getData();
			int packetId = PacketSerializer.readPacketId(serverdata);
			if (packetId == PEPacketIDs.RESOURCE_PACK) {
				plugin.debug("Replacing resource pack data with real information...");
				ResourcePackInfoPacket infoPacket = new ResourcePackInfoPacket(PocketStuffAPI.getResourcePackManager().resourcePacksRequired(), 
						PocketStuffAPI.getResourcePackManager().getBehaviorPacks(), PocketStuffAPI.getResourcePackManager().getResourcePacks());
				event.setData(infoPacket.encode(connection));
				queuePackets = true;
			} else if (packetId == PEPacketIDs.RESOURCE_STACK) {
				if (!connection.hasMetadata(packFinished)) {
					plugin.debug("Cancelling stack data...");
					event.setCancelled(true);
				} else {
					plugin.debug("I'm done with resources! Sending queued packets and removing myself...");
					queue.forEach(packet -> connection.sendRawPacket(MiscSerializer.readAllBytes(packet)));
					connection.removePacketListener(this);
				}
			} else if (queuePackets) {
				plugin.debug("Caching packet to resend after resource handling.");
				queue.add(serverdata.copy());
				event.setCancelled(true);
			}
		}

	}

}