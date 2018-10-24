package protocolsupportpocketstuff.api.util;

import org.bukkit.Bukkit;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import protocolsupport.api.ProtocolSupportAPI;
import protocolsupport.api.ProtocolType;
import protocolsupportpocketstuff.ProtocolSupportPocketStuff;
import protocolsupportpocketstuff.api.modals.Modal;
import protocolsupportpocketstuff.api.modals.ModalType;
import protocolsupportpocketstuff.api.modals.callback.ModalCallback;
import protocolsupportpocketstuff.api.skins.PocketSkinModel;
import protocolsupportpocketstuff.packet.PEPacket;

import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/***
 * Utility class to get and do pocket-only-stuff for pocket 
 */
public class PocketPlayer {

    //=====================================================\\
    //						Getting						   \\
    //=====================================================\\

	/***
	 * Checks if the player is a pocket player.
	 * @param player
	 * @return the truth.
	 */
	public static boolean isPocketPlayer(Player player) {
		return ProtocolSupportAPI.getProtocolVersion(player).getProtocolType().equals(ProtocolType.PE);
	}

	/***
	 * Gets all pocket players on the server.
	 * <br/><br/>
	 * <i>If your goal is sending packets, we advise to use
	 * @return all pocket players.
	 */
	public static Collection<? extends Player> getPocketPlayers() {
		return Bukkit.getOnlinePlayers().stream().filter(pocketFilter()).collect(Collectors.toList());
	}

	/***
	 * Filter to filter PE players.
	 * @return the truth is a predicate.
	 */
	public static Predicate<Player> pocketFilter() {
		return p -> isPocketPlayer(p);
	}

    //=====================================================\\
    //						Packets						   \\
    //=====================================================\\

	/***
	 * Sends a modal to a player and gets the callback id.
	 * <br/><br/>
	 * <i>When sending multiple packets to pocket it is advised
	 * first and then use {@link PocketCon} to send the packets.</i>
	 * @param player
	 * @param modal
	 * @return
	 */
	public static int sendModal(Player player, Modal modal) {
		return PocketCon.sendModal(ProtocolSupportAPI.getConnection(player), modal);
	}

	public static int sendModal(Player player, Modal modal, ModalCallback modalCallback) {
		return PocketCon.sendModal(ProtocolSupportAPI.getConnection(player), modal, modalCallback);
	}

	public static boolean isOnModal(Player player) {
		return PocketCon.isOnModal(ProtocolSupportAPI.getConnection(player));
	}

	/***
	 * Sends a modal with an id specified.
	 * Nonono, don't use custom ids!
	 * If you like you can use this function in combination with
	 * <br/><br/>
	 * <i>When sending multiple packets to pocket it is advised
	 * first and then use {@link PocketCon} to send the packets.</i>
	 * @param player
	 * @param modalId
	 * @param modalJSON
	 * @return the modal's callback id.
	 */
    public static int sendModal(Player player, int modalId, ModalType modalType, String modalJSON) {
        return PocketCon.sendModal(ProtocolSupportAPI.getConnection(player), modalId, modalType, modalJSON);
    }

    public static int sendModal(Player player, int modalId, ModalType modalType, String modalJSON, ModalCallback modalCallback) {
        return PocketCon.sendModal(ProtocolSupportAPI.getConnection(player), modalId, modalType, modalJSON, modalCallback);
	}

	/***
	 * Sends a PocketSkin to a pocket connection.
	 * <br/><br/>
	 * <i>When sending multiple packets to pocket it is advised
	 * first and then use {@link PocketCon} to send the packets.</i>
	 * @param player
	 * @param uuid
	 * @param skin
	 * @param skinModel
	 */
	public static void sendSkin(Player player, UUID uuid, byte[] skin, PocketSkinModel skinModel) {
		PocketCon.sendSkin(ProtocolSupportAPI.getConnection(player), uuid, skin, skinModel);
	}

	/***
	 * Sends a dimension change to a pocket connection.
	 * <br/><br/>
	 * <i>When sending multiple packets to pocket it is advised
	 * first and then use {@link PocketCon} to send the packets.</i>
	 * @param player
	 * @param environment
	 * @param location
	 */
	public static void sendDimensionChange(Player player, Environment environment, Vector location) {
		PocketCon.sendDimensionChange(ProtocolSupportAPI.getConnection(player), environment, location);
	}

	/***
	 * Transfers a player to another server
	 * @param player
	 * @param address
	 * @param port
	 */
	public static void transfer(Player player, String address, short port) {
		PocketCon.transfer(ProtocolSupportAPI.getConnection(player), address, port);
	}

	/***
	 * Gets the client random ID assigned upon install to the user. This can be edited by the client, so beware!
	 * @param player
	 * @return client's random id
	 */
	public static long getClientRandomId(Player player) {
		return PocketCon.getClientRandomId(ProtocolSupportAPI.getConnection(player));
	}

	/***
	 * Gets the client device model
	 * @param player
	 * @return client's device model
	 */
	public static String getDeviceModel(Player player) {
		return PocketCon.getDeviceModel(ProtocolSupportAPI.getConnection(player));
	}

	/***
	 * Gets the client operating system
	 * @param player
	 * @return client's operating system
	 */
	public static DeviceOperatingSystem getOperatingSystem(Player player) {
		return PocketCon.getOperatingSystem(ProtocolSupportAPI.getConnection(player));
	}

	/***
	 * Gets the client version
	 * @param player
	 * @return client version
	 */
	public static String getClientVersion(Player player) {
		return PocketCon.getClientVersion(ProtocolSupportAPI.getConnection(player));
	}

	/***
	 * Gets the client's information map.
	 * @param player
	 * @return
	 */
	public static HashMap<String, Object> getClientInformationMap(Player player) {
		return PocketCon.getClientInformationMap(ProtocolSupportAPI.getConnection(player));
	}

	/***
	 * Gets the client unique identifier
	 * <br/><br/>
	 * <b>This isn't the server unique identifier for the player</b>, this unique ID is sent by the client during login and
	 * it is used for skin updates, player list updates and other misc stuff.
	 * @param player
	 * @return the client unique identifier
	 */
	public static UUID getClientUniqueId(Player player) {
		return PocketCon.getClientUniqueId(ProtocolSupportAPI.getConnection(player));
	}

	/***
	 * Sends a packet to pocket.
	 * <br/><br/>
	 * <i>When sending multiple packets to pocket it is advised
	 * first and then use {@link PocketCon} to send the packets.</i>
	 * @param player
	 * @param packet
	 */
	public static void sendPocketPacket(Player player, PEPacket packet) {
		PocketCon.sendPocketPacket(ProtocolSupportAPI.getConnection(player), packet);
	}

	// ACTION BUTTON

    public void sendButton(Player player, String buttonText, int displayTick, Runnable click) {
        ProtocolSupportPocketStuff.getInstance().getActionButton().sendButton(player, buttonText, displayTick, click);
    }

    public void sendButton(Player player, String buttonText, Runnable click) {
        ProtocolSupportPocketStuff.getInstance().getActionButton().sendButton(player, buttonText, click);
    }

    public boolean hasButton(Player player) {
        return ProtocolSupportPocketStuff.getInstance().getActionButton().hasButton(player);
    }

    public void closeButton(Player player) {
        ProtocolSupportPocketStuff.getInstance().getActionButton().closeButton(player);
    }

    public void changeButtonText(Player player, String value) {
        ProtocolSupportPocketStuff.getInstance().getActionButton().changeText(player, value);
    }

}
