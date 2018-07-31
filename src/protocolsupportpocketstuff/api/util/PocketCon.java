package protocolsupportpocketstuff.api.util;

import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.World.Environment;
import org.bukkit.util.Vector;
import protocolsupport.api.Connection;
import protocolsupport.api.ProtocolSupportAPI;
import protocolsupport.api.ProtocolType;
import protocolsupport.libs.com.google.gson.JsonArray;
import protocolsupport.protocol.serializer.MiscSerializer;
import protocolsupportpocketstuff.api.event.ComplexFormResponseEvent;
import protocolsupportpocketstuff.api.event.ModalResponseEvent;
import protocolsupportpocketstuff.api.event.ModalWindowResponseEvent;
import protocolsupportpocketstuff.api.event.SimpleFormResponseEvent;
import protocolsupportpocketstuff.api.modals.Modal;
import protocolsupportpocketstuff.api.modals.ModalType;
import protocolsupportpocketstuff.api.modals.callback.ComplexFormCallback;
import protocolsupportpocketstuff.api.modals.callback.ModalCallback;
import protocolsupportpocketstuff.api.modals.callback.ModalWindowCallback;
import protocolsupportpocketstuff.api.modals.callback.SimpleFormCallback;
import protocolsupportpocketstuff.api.skins.PocketSkinModel;
import protocolsupportpocketstuff.packet.PEPacket;
import protocolsupportpocketstuff.packet.play.DimensionPacket;
import protocolsupportpocketstuff.packet.play.ModalRequestPacket;
import protocolsupportpocketstuff.packet.play.ServerSettingsRequestPacket;
import protocolsupportpocketstuff.packet.play.SkinPacket;
import protocolsupportpocketstuff.packet.play.TransferPacket;
import protocolsupportpocketstuff.storage.Modals;
import protocolsupportpocketstuff.util.StuffUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class PocketCon {

	public static final String META_ON_MODAL = "__PS_ON_MODAL";

	//=====================================================\\
    //						Getting						   \\
    //=====================================================\\

	/***
	 * Checks if a connection is a pocket connection.
	 * @return the truth.
	 */
	public static boolean isPocketConnection(Connection connection) {
		return connection.getVersion().getProtocolType().equals(ProtocolType.PE);
	}
	
	/**
	 * Gets all pocket connections on the server.
	 * @return all pocket connections.
	 */
	public static Collection<? extends Connection> getPocketConnections() {
		return ProtocolSupportAPI.getConnections().stream().filter(pocketFilter()).collect(Collectors.toList());
	}
	
	/***
	 * Filter to filter PE connections.
	 * @return the truth is a predicate.
	 */
	public static Predicate<Connection> pocketFilter() {
		return c -> isPocketConnection(c);
	}
	
    //=====================================================\\
    //						Packets						   \\
    //=====================================================\\
	
	/***
	 * Sends a modal and gets the corresponding id.
	 * @param modal
	 * @return
	 */
	public static int sendModal(Connection connection, Modal modal) {
		return sendModal(connection, Modals.INSTANCE.takeId(), modal.getType(), modal.toJSON(), null);
	}

	public static int sendModal(Connection connection, Modal modal, ModalCallback callback) {
		return sendModal(connection, Modals.INSTANCE.takeId(), modal.getType(), modal.toJSON(), callback);
	}

	/***
	 * Sends a modal with an id specified.
	 * Nonono, don't use custom ids!
	 * If you like you can use this function in combination with
	 * @param id
	 * @return the modal's callback id.
	 */
	public static int sendModal(Connection connection, int id, ModalType modalType, String modalJSON) {
		sendModal(connection, id, modalType, modalJSON, null);
		return id;
	}

	public static int sendModal(Connection connection, int id, ModalType modalType, String modalJSON, ModalCallback callback) {
		if (callback != null)
			addCallback(connection, id, callback);

		connection.addMetadata("modalType", modalType);
		sendPocketPacket(connection, new ModalRequestPacket(id, modalJSON));
		return id;
	}

	public static void addModalType(Connection connection, ModalType type) {
		connection.addMetadata("modalType", type);
	}

	public static ModalType getModalType(Connection connection) {
		return (ModalType) connection.getMetadata("modalType");
	}

	public static void addCallback(Connection connection, int id, ModalCallback callback) {
		connection.addMetadata("modalCallback", callback);
	}

	public static ModalCallback getCallback(Connection connection) {
		return (ModalCallback) connection.getMetadata("modalCallback");
	}

	public static void removeCallback(Connection connection) {
		connection.removeMetadata("modalCallback");
	}

	public static void handleModalResponse(Connection connection, ModalResponseEvent event) {
		ModalCallback modalCallback = PocketCon.getCallback(connection);

		if (modalCallback == null) {
			if (!connection.hasMetadata(ServerSettingsRequestPacket.META_KEY)) {
				return;
			}
			Pair<Integer, ModalCallback> metadata = (Pair<Integer, ModalCallback>) connection.getMetadata(ServerSettingsRequestPacket.META_KEY);
			if (metadata.getKey().intValue() != event.getModalId()) {
				return;
			}
			modalCallback = metadata.getValue();
			connection.removeMetadata(ServerSettingsRequestPacket.META_KEY);
		}

		PocketCon.removeCallback(connection);

		connection.addMetadata(META_ON_MODAL, "");

		try {
			modalCallback.onModalResponse(connection.getPlayer(), event.getModalJSON(), event.isCancelled());
			if (modalCallback instanceof SimpleFormCallback) {
				SimpleFormCallback simpleFormCallback = (SimpleFormCallback) modalCallback;
				int clickedButton = event instanceof SimpleFormResponseEvent ? ((SimpleFormResponseEvent) event).getClickedButton() : -1;
				simpleFormCallback.onSimpleFormResponse(connection.getPlayer(), event.getModalJSON(), event.isCancelled(), clickedButton);
			} else if (modalCallback instanceof ComplexFormCallback) {
				ComplexFormCallback complexFormCallback = (ComplexFormCallback) modalCallback;
				JsonArray jsonArray = event instanceof ComplexFormResponseEvent ? ((ComplexFormResponseEvent) event).getJsonArray() : null;
				complexFormCallback.onComplexFormResponse(connection.getPlayer(), event.getModalJSON(), event.isCancelled(), jsonArray);
			} else if (modalCallback instanceof ModalWindowCallback) {
				ModalWindowCallback modalWindowResponseEvent = (ModalWindowCallback) modalCallback;
				boolean result = event instanceof ModalWindowResponseEvent && ((ModalWindowResponseEvent) event).getResult();
				modalWindowResponseEvent.onModalWindowResponse(connection.getPlayer(), event.getModalJSON(), event.isCancelled(), result);
			}
		} finally {
			connection.removeMetadata(META_ON_MODAL);
		}
	}

	public static boolean isOnModal(Connection connection) {
		return connection.hasMetadata(META_ON_MODAL);
	}

	/***
	 * Sends a PocketSkin to a pocket connection.
	 * @param connection
	 * @param uuid
	 * @param skin
	 * @param skinModel
	 */
	public static void sendSkin(Connection connection, UUID uuid, byte[] skin, PocketSkinModel skinModel) {
		//TODO: "Steve" is actually a hack. The name send should be the previous skin name. Not sure if this matters though. Works for now :S"
		sendPocketPacket(connection, new SkinPacket(uuid, skinModel.getSkinId(), skinModel.getSkinName(), "Steve", skin, new byte[0], skinModel.getGeometryId(), skinModel.getGeometryData()));
	}
	
	/***
	 * Sends a dimension change to a pocket connection.
	 * @param connection
	 * @param environment
	 * @param location
	 */
	public static void sendDimensionChange(Connection connection, Environment environment, Vector location) {
		sendPocketPacket(connection, new DimensionPacket(environment, location));
	}

	/***
	 * Transfers a player to another server
	 * @param connection
	 * @param address
	 * @param port
	 */
	public static void transfer(Connection connection, String address, short port) {
		sendPocketPacket(connection, new TransferPacket(address, port));
	}

	/***
	 * Gets the client random ID assigned upon install to the user. This can be edited by the client, so beware!
	 * @param connection
	 * @return client's random id
	 */
	public static long getClientRandomId(Connection connection) {
		return (Long) getClientInformationMap(connection).get("ClientRandomId");
	}

	/***
	 * Gets the client device model
	 * @param connection
	 * @return client's device model
	 */
	public static String getDeviceModel(Connection connection) {
		return (String) getClientInformationMap(connection).get("DeviceModel");
	}

	/***
	 * Gets the client operating system
	 * @param connection
	 * @return client's operating system
	 */
	public static DeviceOperatingSystem getOperatingSystem(Connection connection) {
		return DeviceOperatingSystem.getOperatingSystemById((int) getClientInformationMap(connection).get("DeviceOS"));
	}

	/***
	 * Gets the client version
	 * @param connection
	 * @return client version
	 */
	public static String getClientVersion(Connection connection) {
		return (String) getClientInformationMap(connection).get("GameVersion");
	}

	/***
	 * Gets the client's information map.
	 * @param connection
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static HashMap<String, Object> getClientInformationMap(Connection connection) {
		return (HashMap<String, Object>) connection.getMetadata(StuffUtils.CLIENT_INFO_KEY);
	}

	/***
	 * Gets the client unique identifier
	 * <br/><br/>
	 * <b>This isn't the server unique identifier for the player</b>, this unique ID is sent by the client during login and
	 * it is used for skin updates, player list updates and other misc stuff.
	 * @param connection
	 * @return the client unique identifier
	 */
	public static UUID getClientUniqueId(Connection connection) {
		return (UUID) connection.getMetadata(StuffUtils.CLIENT_UUID_KEY);
	}

	/***
	 * Sends a packet to pocket.
	 * @param connection
	 * @param packet
	 */
	public static void sendPocketPacket(Connection connection, PEPacket packet) {
		connection.sendRawPacket(MiscSerializer.readAllBytes(packet.encode(connection)));
	}
	
}
