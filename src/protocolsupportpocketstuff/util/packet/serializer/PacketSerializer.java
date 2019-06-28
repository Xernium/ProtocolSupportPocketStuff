package protocolsupportpocketstuff.util.packet.serializer;

import io.netty.buffer.ByteBuf;
import protocolsupport.protocol.serializer.VarNumberSerializer;

public class PacketSerializer {

	public static int readPacketId(ByteBuf from) {
		int id = VarNumberSerializer.readVarInt(from);
		return id;
	}

}
