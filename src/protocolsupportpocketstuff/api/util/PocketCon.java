package protocolsupportpocketstuff.api.util;

import org.bukkit.World.Environment;
import org.bukkit.util.Vector;
import protocolsupport.api.Connection;
import protocolsupport.api.ProtocolSupportAPI;
import protocolsupport.api.ProtocolType;
import protocolsupport.protocol.typeremapper.pe.PEModel;
import protocolsupportpocketstuff.api.modals.Modal;
import protocolsupportpocketstuff.api.modals.ModalType;
import protocolsupportpocketstuff.api.modals.callback.ModalCallback;
import protocolsupportpocketstuff.packet.PEPacket;
import protocolsupportpocketstuff.packet.play.DimensionPacket;
import protocolsupportpocketstuff.packet.play.ModalRequestPacket;
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
		return ProtocolSupportAPI.getConnections().stream().filter(PocketCon::isPocketConnection).collect(Collectors.toList());
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
		if (callback != null) {
			modalType.requireType(callback);
			addCallback(connection, id, callback);
		}

		connection.addMetadata("modalType", modalType);
		connection.addMetadata(META_ON_MODAL, "");
		sendPocketPacket(connection, new ModalRequestPacket(id, modalJSON));
		return id;
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

	public static ModalCallback retrieveCallback(Connection connection) {
		ModalCallback callback = getCallback(connection);
		if (callback == null) {
			return null;
		}
		removeCallback(connection);
		return callback;
	}

	public static boolean isOnModal(Connection connection) {
		return connection.hasMetadata(META_ON_MODAL);
	}

	/**
	 * Sends skin info to a pocket connection. May crash client if target player not exists.
	 * @param uuid target player id
	 */
	public static void sendSkin(Connection connection, UUID uuid, String skinId, PEModel model) {
		sendPocketPacket(connection, new SkinPacket(uuid, skinId, model.getSkinName(), "Steve", model.getSkinData(), model.getCapeData(), model.getGeometryId(), model.getGeometryData()));
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
		if (connection != null && connection.getVersion().getProtocolType() == ProtocolType.PE) {
			connection.sendRawPacket(packet.encode(connection));
		}
    }

}
