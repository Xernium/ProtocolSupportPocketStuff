package protocolsupportpocketstuff.packet.play;

import io.netty.buffer.ByteBuf;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import protocolsupport.api.Connection;
import protocolsupportpocketstuff.ProtocolSupportPocketStuff;
import protocolsupportpocketstuff.api.event.ServerSettingsRequestEvent;
import protocolsupportpocketstuff.api.modals.Modal;
import protocolsupportpocketstuff.api.modals.callback.ModalCallback;
import protocolsupportpocketstuff.api.util.PocketCon;
import protocolsupportpocketstuff.packet.PEPacket;
import protocolsupportpocketstuff.storage.Modals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerSettingsRequestPacket extends PEPacket {

    public static final String META_KEY = "SERVER_SETTINGS_CALLBACK";

    @Override
    public int getPacketId() {
        return 102;
    }

    @Override
    public void toData(Connection connection, ByteBuf serializer) {

    }

    @Override
    public void readFromClientData(Connection connection, ByteBuf clientdata) {

    }

    public class decodeHandler extends PEPacket.decodeHandler {

        public decodeHandler(ProtocolSupportPocketStuff plugin, Connection connection) {
            super(plugin, connection);
        }

        @Override
        public void handle() {
            ServerSettingsRequestEvent event = new ServerSettingsRequestEvent(connection);
            pm.callEvent(event);
            Pair<Modal, ModalCallback> settings = event.getSettings();
            if (settings == null) {
                return;
            }
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                int id = Modals.INSTANCE.takeId();
                Modal modal = settings.getKey();
                ServerSettingsResponsePacket packet = new ServerSettingsResponsePacket(id, modal.toJSON());
                PocketCon.sendPocketPacket(connection, packet);
                connection.addMetadata(META_KEY, Pair.of(id, settings.getValue()));
            }, 10);
        }
    }
}
