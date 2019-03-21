package protocolsupportpocketstuff.packet.play;

import io.netty.buffer.ByteBuf;
import org.apache.commons.lang3.tuple.Pair;
import protocolsupport.api.Connection;
import protocolsupport.libs.com.google.gson.JsonElement;
import protocolsupport.protocol.serializer.StringSerializer;
import protocolsupport.protocol.serializer.VarNumberSerializer;
import protocolsupport.protocol.typeremapper.pe.PEPacketIDs;
import protocolsupportpocketstuff.ProtocolSupportPocketStuff;
import protocolsupportpocketstuff.api.modals.ModalType;
import protocolsupportpocketstuff.api.modals.callback.ComplexFormCallback;
import protocolsupportpocketstuff.api.modals.callback.GenericModalCallback;
import protocolsupportpocketstuff.api.modals.callback.ModalCallback;
import protocolsupportpocketstuff.api.modals.callback.ModalWindowCallback;
import protocolsupportpocketstuff.api.modals.callback.SimpleFormCallback;
import protocolsupportpocketstuff.api.util.PocketCon;
import protocolsupportpocketstuff.packet.PEPacket;
import protocolsupportpocketstuff.util.StuffUtils;

import static protocolsupportpocketstuff.api.util.PocketCon.META_ON_MODAL;

public class ModalResponsePacket extends PEPacket {

    private int modalId;
    private String modalJSON;

    public ModalResponsePacket() {
    }

    public ModalResponsePacket(int modalId, String modalJSON) {
        this.modalId = modalId;
        this.modalJSON = modalJSON;
    }

    @Override
    public int getPacketId() {
        return PEPacketIDs.MODAL_RESPONSE;
    }

    @Override
    public void toData(Connection connection, ByteBuf serializer) {
        VarNumberSerializer.writeVarInt(serializer, modalId);
        StringSerializer.writeString(serializer, connection.getVersion(), modalJSON);
    }

    @Override
    public void readFromClientData(Connection connection, ByteBuf clientData) {
        modalId = VarNumberSerializer.readVarInt(clientData);
        modalJSON = StringSerializer.readString(clientData, connection.getVersion());
    }

    public int getModalId() {
        return modalId;
    }

    public String getModalJSON() {
        return modalJSON;
    }

    public class decodeHandler extends PEPacket.decodeHandler {

        public decodeHandler(ProtocolSupportPocketStuff plugin, Connection connection) {
            super(plugin, connection);
        }

        @Override
        public void handle() {
            ModalCallback modalCallback = PocketCon.retrieveCallback(connection);

            if (modalCallback == null) {
                if (!connection.hasMetadata(ServerSettingsRequestPacket.META_KEY)) {
                    return;
                }

                Pair<Integer, ModalCallback> settings = (Pair<Integer, ModalCallback>) connection.getMetadata(ServerSettingsRequestPacket.META_KEY);
                connection.removeMetadata(ServerSettingsRequestPacket.META_KEY);

                if (settings.getKey() != modalId) {
                    return;
                }

                modalCallback = settings.getValue();
            }

            try {
                JsonElement modalRes = StuffUtils.JSON_PARSER.parse(modalJSON);
                ModalType modalType = modalCallback.getModalType();
                switch (modalType) {
                    case GENERIC_MODAL:
                        GenericModalCallback generic = (GenericModalCallback) modalCallback;
                        generic.onGenericModalResponse(connection.getPlayer(), modalJSON, modalRes.isJsonNull());
                        break;
                    case MODAL_WINDOW:
                        ModalWindowCallback windowCallback = (ModalWindowCallback) modalCallback;
                        windowCallback.onModalWindowResponse(connection.getPlayer(), modalJSON, modalRes.isJsonNull(), modalRes.isJsonNull() ? false : modalRes.getAsBoolean());
                        break;
                    case SIMPLE_FORM:
                        SimpleFormCallback simpleFormCallback = (SimpleFormCallback) modalCallback;
                        simpleFormCallback.onSimpleFormResponse(connection.getPlayer(), modalJSON, modalRes.isJsonNull(), modalRes.isJsonNull() ? -1 : modalRes.getAsInt());
                        break;
                    case COMPLEX_FORM:
                        ComplexFormCallback complexFormCallback = (ComplexFormCallback) modalCallback;
                        complexFormCallback.onComplexFormResponse(connection.getPlayer(), modalJSON, modalRes.isJsonNull(), modalRes.isJsonNull() ? null : modalRes.getAsJsonArray());
                        break;
                }
            } finally {
                connection.removeMetadata(META_ON_MODAL);
            }
            }
    }
}
