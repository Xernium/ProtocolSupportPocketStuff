package protocolsupportpocketstuff.packet.play;

import io.netty.buffer.ByteBuf;
import protocolsupport.api.Connection;
import protocolsupport.protocol.serializer.VarInt;
import protocolsupport.protocol.serializer.VarNumberSerializer;
import protocolsupport.protocol.typeremapper.pe.PEPacketIDs;
import protocolsupportpocketstuff.packet.PEPacket;

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
		VarInt.writeUnsignedVarLong(serializer, -1);// Fix riding runtime id
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
		VarNumberSerializer.readVarInt(clientdata);
		if (mode == 2) {
			VarNumberSerializer.readSVarInt(clientdata);
			VarNumberSerializer.readSVarInt(clientdata);
		}
	}

	public long getEntityId() {
		return entityId;
	}

	public float getX() {
		return x;
	}

	public float getY() {
		return y;
	}

	public float getZ() {
		return z;
	}

	public float getPitch() {
		return pitch;
	}

	public float getHeadYaw() {
		return headYaw;
	}

	public float getYaw() {
		return yaw;
	}

	public int getMode() {
		return mode;
	}

	public boolean isOnGround() {
		return onGround;
	}

}