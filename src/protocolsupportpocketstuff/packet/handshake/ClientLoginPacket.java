package protocolsupportpocketstuff.packet.handshake;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import org.json.simple.JSONObject;
import protocolsupport.api.Connection;
import protocolsupport.api.ProtocolType;
import protocolsupport.protocol.serializer.ArraySerializer;
import protocolsupport.protocol.typeremapper.pe.PEPacketIDs;
import protocolsupportpocketstuff.ProtocolSupportPocketStuff;
import protocolsupportpocketstuff.packet.PEPacket;
import protocolsupportpocketstuff.storage.Skins;
import protocolsupportpocketstuff.util.MineskinThread;
import protocolsupportpocketstuff.util.StuffUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Base64;
import java.util.HashMap;
import java.util.UUID;

public class ClientLoginPacket extends PEPacket {
	int protocolVersion;
	JSONObject clientPayload;

	public ClientLoginPacket() { }

	@Override
	public int getPacketId() {
		return PEPacketIDs.LOGIN;
	}

	@Override
	public void toData(Connection connection, ByteBuf serializer) {

	}

	@SuppressWarnings("resource")
	@Override
	public void readFromClientData(Connection connection, ByteBuf clientData) {
		protocolVersion = clientData.readInt(); //protocol version

		ByteBuf logindata = Unpooled.wrappedBuffer(ArraySerializer.readByteArray(clientData, connection.getVersion()));

		// skip chain data
		logindata.skipBytes(logindata.readIntLE());

		// decode skin data
		try {
			InputStream inputStream = new ByteBufInputStream(logindata, logindata.readIntLE());
			ByteArrayOutputStream result = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			int length;
			while ((length = inputStream.read(buffer)) != -1) {
				result.write(buffer, 0, length);
			}
			clientPayload = decodeToken(result.toString("UTF-8"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private JSONObject decodeToken(String token) {
		String[] base = token.split("\\.");
		if (base.length < 2) {
			return null;
		}
		return StuffUtils.GSON.fromJson(new InputStreamReader(new ByteArrayInputStream(Base64.getDecoder().decode(base[1]))), JSONObject.class);
	}

	public class decodeHandler extends PEPacket.decodeHandler {

		public decodeHandler(ProtocolSupportPocketStuff plugin, Connection connection) {
			super(plugin, connection);
		}

		@Override
		public void onRawPacketReceiving(RawPacketEvent e) {
			//Custom prevention logic because we couldn't get the version yet.
			if(connection.getVersion() == null) {
				return;
			} else if(connection.getVersion().getProtocolType() != ProtocolType.PE) {
				connection.removePacketListener(this);
				return;
			}
			super.onRawPacketReceiving(e);
		}

		@Override
		public void handle() {
			ClientLoginPacket clientLoginPacket = ClientLoginPacket.this;
			JSONObject clientPayload = clientLoginPacket.clientPayload;

			if (clientPayload == null) {
				return;
			}

			HashMap<String, Object> clientInfo = new HashMap<>();
			// "In general you shouldn't really expect the payload to be sent with psbpe" -Shevchik
				clientInfo.putAll(clientPayload);

			connection.addMetadata(StuffUtils.CLIENT_INFO_KEY, clientInfo);

			if (!clientPayload.containsKey("SkinData")) {
				return;
			}

			String skinData = String.valueOf(clientPayload.get("SkinData"));
			String uniqueSkinId = UUID.nameUUIDFromBytes(skinData.getBytes()).toString();

			if (Skins.INSTANCE.hasPcSkin(uniqueSkinId)) {
				plugin.debug("Already cached skin, adding to the Connection's metadata...");
				connection.addMetadata(StuffUtils.APPLY_SKIN_ON_JOIN_KEY, Skins.INSTANCE.getPcSkin(uniqueSkinId));
				return;
			}
			byte[] skinByteArray = Base64.getDecoder().decode(skinData);

			MineskinThread mineskinThread = new MineskinThread(plugin, connection, uniqueSkinId, skinByteArray, String.valueOf(clientPayload.get("SkinGeometryName")).equals("geometry.humanoid.customSlim"));
			mineskinThread.start();
		}
	}
}
