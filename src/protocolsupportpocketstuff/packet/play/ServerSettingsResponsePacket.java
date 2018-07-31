package protocolsupportpocketstuff.packet.play;

import io.netty.buffer.ByteBuf;
import protocolsupport.api.Connection;
import protocolsupport.protocol.serializer.StringSerializer;
import protocolsupport.protocol.serializer.VarNumberSerializer;
import protocolsupportpocketstuff.packet.PEPacket;

public class ServerSettingsResponsePacket extends PEPacket {

    protected int id;
    protected String payload;

    public ServerSettingsResponsePacket() {
    }

    public ServerSettingsResponsePacket(int id, String payload) {
        this.id = id;
        this.payload = payload;
    }

    @Override
    public int getPacketId() {
        return 103;
    }

    @Override
    public void toData(Connection connection, ByteBuf serializer) {
        VarNumberSerializer.writeVarInt(serializer, id);
        StringSerializer.writeString(serializer, connection.getVersion(), payload);
    }

    @Override
    public void readFromClientData(Connection connection, ByteBuf clientdata) {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
