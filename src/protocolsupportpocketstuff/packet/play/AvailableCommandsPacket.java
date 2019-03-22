package protocolsupportpocketstuff.packet.play;

import com.google.common.collect.ImmutableMap;
import io.netty.buffer.ByteBuf;
import protocolsupport.api.Connection;
import protocolsupport.api.ProtocolVersion;
import protocolsupport.protocol.serializer.StringSerializer;
import protocolsupport.protocol.serializer.VarNumberSerializer;
import protocolsupport.protocol.typeremapper.pe.PEPacketIDs;
import protocolsupportpocketstuff.packet.PEPacket;

import java.util.Map;

public class AvailableCommandsPacket extends PEPacket {

    private Map<String, String> commands;

    public AvailableCommandsPacket(Map<String, String> commands) {
        this.commands = commands;
    }

    public AvailableCommandsPacket() {
        this(ImmutableMap.of());
    }

    @Override
    public int getPacketId() {
        return PEPacketIDs.AVAILABLE_COMMANDS;
    }

    @Override
    public void toData(Connection connection, ByteBuf buf) {
        VarNumberSerializer.writeVarInt(buf, 0);
        VarNumberSerializer.writeVarInt(buf, 0);
        VarNumberSerializer.writeVarInt(buf, 0);
        VarNumberSerializer.writeVarInt(buf, commands.size());// elements length
        commands.forEach((command, s2) -> writeCommands(buf, command, s2));
    }

    private void writeCommands(ByteBuf buf, String command, String description) {
        StringSerializer.writeString(buf, ProtocolVersion.MINECRAFT_PE, command);
        StringSerializer.writeString(buf, ProtocolVersion.MINECRAFT_PE, description);
        buf.writeByte(0);
        buf.writeByte(0);
        buf.writeIntLE(-1);
        VarNumberSerializer.writeVarInt(buf, 0);
    }

    @Override
    public void readFromClientData(Connection connection, ByteBuf clientdata) {

    }

}
