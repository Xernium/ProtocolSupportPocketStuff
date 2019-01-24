package protocolsupportpocketstuff.packet.play;

import io.netty.buffer.ByteBuf;
import protocolsupport.api.Connection;
import protocolsupport.protocol.packet.middleimpl.clientbound.play.v_pe.EntityMetadata;
import protocolsupport.protocol.serializer.VarNumberSerializer;
import protocolsupport.protocol.typeremapper.pe.PEPacketIDs;
import protocolsupport.protocol.utils.datawatcher.DataWatcherObject;
import protocolsupport.protocol.utils.datawatcher.DataWatcherObjectIdRegistry;
import protocolsupport.utils.CollectionsUtils;
import protocolsupportpocketstuff.packet.PEPacket;

import java.util.ArrayList;
import java.util.List;

public class EntityDataPacket extends PEPacket {
	private long entityId;
	private List<EntityMetadata.EntityDataEntry> entries;

	public EntityDataPacket() { }

	public EntityDataPacket(long entityId, CollectionsUtils.ArrayMap<DataWatcherObject<?>> metadata) {
		this.entityId = entityId;
		this.entries = new ArrayList<>();
		for (int i = metadata.getMinKey(); i < metadata.getMaxKey(); i++) {
			DataWatcherObject<?> object = metadata.get(i);
			if (object != null) {
				this.entries.add(new EntityMetadata.EntityDataEntry(EntityMetadata.EntityDataRegistry.values()[i], object));
			}
		}
	}

	public EntityDataPacket(long entityId, List<EntityMetadata.EntityDataEntry> metadata) {
		this.entityId = entityId;
		this.entries = metadata;
	}

	@Override
	public int getPacketId() {
		return PEPacketIDs.SET_ENTITY_DATA;
	}

	@Override
	public void toData(Connection connection, ByteBuf data) {
        VarNumberSerializer.writeVarLong(data, entityId);
        VarNumberSerializer.writeVarInt(data, entries.size());
        for (EntityMetadata.EntityDataEntry entry : entries) {
            VarNumberSerializer.writeVarInt(data, entry.getId().getDataValue(connection.getVersion()));
            VarNumberSerializer.writeVarInt(data, DataWatcherObjectIdRegistry.getDataTypeId(entry.getObject(), connection.getVersion()));
            entry.getObject().writeToStream(data, connection.getVersion(), "en_us");
        }
    }

	@Override
	public void readFromClientData(Connection connection, ByteBuf clientdata) {
		this.entityId = VarNumberSerializer.readVarLong(clientdata);
		clientdata.skipBytes(clientdata.readableBytes());
	}

	public long getEntityId() {
		return entityId;
	}

}