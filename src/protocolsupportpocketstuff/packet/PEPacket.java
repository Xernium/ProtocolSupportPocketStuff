package protocolsupportpocketstuff.packet;

import io.netty.buffer.ByteBuf;
import org.bukkit.plugin.PluginManager;
import protocolsupport.api.Connection;
import protocolsupport.api.Connection.PacketListener;
import protocolsupport.protocol.pipeline.version.v_pe.PEPacketEncoder;
import protocolsupport.protocol.serializer.VarNumberSerializer;
import protocolsupport.utils.netty.Allocator;
import protocolsupportpocketstuff.ProtocolSupportPocketStuff;

public abstract class PEPacket {

	public abstract int getPacketId();
	
	public abstract void toData(Connection connection, ByteBuf serializer);
	
	public abstract void readFromClientData(Connection connection, ByteBuf clientdata);
		
	public ByteBuf encode(Connection connection) {
		ByteBuf serializer = Allocator.allocateBuffer();
		PEPacketEncoder.sWritePacketId(connection.getVersion(), serializer, getPacketId());
		toData(connection, serializer);
		return serializer;
	}
	
	public void decode(Connection connection, ByteBuf buf) {
		readFromClientData(connection, buf);
	}
	
	public abstract class decodeHandler extends PacketListener {
		
		protected ProtocolSupportPocketStuff plugin;
		protected Connection connection;
		protected PluginManager pm;
		
		public decodeHandler(ProtocolSupportPocketStuff plugin, Connection connection) {
			this.plugin = plugin;
			this.connection = connection;
			this.pm = plugin.getServer().getPluginManager();
		}
		
		public void onRawPacketReceiving(RawPacketEvent e) {
			ByteBuf clientData = e.getData();
			if(VarNumberSerializer.readVarInt(clientData) == PEPacket.this.getPacketId()) {
				PEPacket.this.decode(connection, clientData);
				handle();
			}
		}
		
		public abstract void handle();
		
	}
	
}
