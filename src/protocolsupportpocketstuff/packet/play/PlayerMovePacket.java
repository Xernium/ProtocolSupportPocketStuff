package protocolsupportpocketstuff.packet.play;

import io.netty.buffer.ByteBuf;
import lombok.Data;
import protocolsupport.api.Connection;
import protocolsupport.protocol.serializer.VarInt;
import protocolsupport.protocol.serializer.VarNumberSerializer;
import protocolsupport.protocol.typeremapper.pe.PEPacketIDs;
import protocolsupportpocketstuff.packet.PEPacket;

@Data
public class PlayerMovePacket extends PEPacket {
	private long entityId;
	private float x;
	private float y;
	private float z;
	private float pitch;
	private float yaw;
	private float headYaw;
	private int mode;
	private boolean onGround;
	private long riding = -1;// Use -1 avoid some issues

	public PlayerMovePacket(long entityId, float x, float y, float z, float pitch, float yaw, float headYaw, int mode, boolean onGround, long riding) {
		this.entityId = entityId;
		this.x = x;
		this.y = y;
		this.z = z;
		this.pitch = pitch;
		this.yaw = yaw;
		this.headYaw = headYaw;
		this.mode = mode;
		this.onGround = onGround;
		this.riding = riding;
	}

	public PlayerMovePacket(long entityId, float x, float y, float z, float pitch, float yaw, float headYaw, int mode, boolean onGround) {
		this.entityId = entityId;
		this.x = x;
		this.y = y;
		this.z = z;
		this.pitch = pitch;
		this.yaw = yaw;
		this.headYaw = headYaw;
		this.mode = mode;
		this.onGround = onGround;
	}

	@Override
	public int getPacketId() {
		return PEPacketIDs.PLAYER_MOVE;
	}

	@Override
	public void toData(Connection connection, ByteBuf serializer) {
		VarInt.writeUnsignedVarLong(serializer, entityId);
		serializer.writeFloatLE(x);
		serializer.writeFloatLE(y);
		serializer.writeFloatLE(z);
		serializer.writeFloatLE(pitch);
		serializer.writeFloatLE(yaw);
		serializer.writeFloatLE(headYaw);
		serializer.writeByte(mode);
		serializer.writeBoolean(onGround); //on ground
		VarInt.writeUnsignedVarLong(serializer, riding);
	}

	@Override
	public void readFromClientData(Connection connection, ByteBuf clientdata) {
		this.entityId = VarNumberSerializer.readSVarLong(clientdata);
		this.x = clientdata.readFloatLE();
		this.y = clientdata.readFloatLE();
		this.z = clientdata.readFloatLE();
		this.pitch = clientdata.readFloatLE();
		this.headYaw = clientdata.readFloatLE();
		this.mode = clientdata.readByte();
		this.onGround = clientdata.readBoolean();
		this.riding = VarNumberSerializer.readVarInt(clientdata);
		if (mode == 2) {
			VarNumberSerializer.readSVarInt(clientdata);
			VarNumberSerializer.readSVarInt(clientdata);
		}
	}

}