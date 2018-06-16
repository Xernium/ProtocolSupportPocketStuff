package protocolsupportpocketstuff.hacks.holograms;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import protocolsupport.api.Connection;
import protocolsupport.api.ProtocolVersion;
import protocolsupport.protocol.packet.middleimpl.clientbound.play.v_pe.EntityMetadata;
import protocolsupport.protocol.packet.middleimpl.clientbound.play.v_pe.SetPosition;
import protocolsupport.protocol.serializer.StringSerializer;
import protocolsupport.protocol.serializer.VarNumberSerializer;
import protocolsupport.protocol.typeremapper.pe.PEPacketIDs;
import protocolsupport.protocol.utils.datawatcher.DataWatcherObject;
import protocolsupport.protocol.utils.datawatcher.ReadableDataWatcherObject;
import protocolsupport.protocol.utils.datawatcher.objects.DataWatcherObjectByte;
import protocolsupport.protocol.utils.datawatcher.objects.DataWatcherObjectFloatLe;
import protocolsupport.protocol.utils.datawatcher.objects.DataWatcherObjectItemStack;
import protocolsupport.protocol.utils.datawatcher.objects.DataWatcherObjectPosition;
import protocolsupport.protocol.utils.datawatcher.objects.DataWatcherObjectSVarLong;
import protocolsupport.protocol.utils.datawatcher.objects.DataWatcherObjectString;
import protocolsupport.protocol.utils.datawatcher.objects.DataWatcherObjectVarInt;
import protocolsupport.protocol.utils.i18n.I18NData;
import protocolsupport.protocol.utils.types.networkentity.NetworkEntity;
import protocolsupport.utils.CollectionsUtils;
import protocolsupportpocketstuff.api.util.PocketCon;
import protocolsupportpocketstuff.packet.play.EntityDestroyPacket;
import protocolsupportpocketstuff.packet.play.PlayerMovePacket;
import protocolsupportpocketstuff.packet.play.SpawnPlayerPacket;

import java.util.HashMap;
import java.util.UUID;

public class HologramsPacketListener extends Connection.PacketListener {
    private Connection con;
    private HashMap<Long, CachedArmorStand> cachedArmorStands = new HashMap<>();
    private static final HashMap<Integer, ReadableDataWatcherObject<?>> DATA_WATCHERS = new HashMap<>();
    private static final int ARMOR_STAND_ID = 61;
    private static final double Y_OFFSET = 1.6200000047683716D;

    static {
        DATA_WATCHERS.put(0, new DataWatcherObjectByte());
        DATA_WATCHERS.put(1, new ReadableDataWatcherObject<Short>() {
            public void readFromStream(ByteBuf byteBuf, ProtocolVersion protocolVersion, String s) throws DecoderException {
                value = byteBuf.readShortLE();
            }

            public void writeToStream(ByteBuf byteBuf, ProtocolVersion protocolVersion, String s) {
                byteBuf.writeShortLE(value);
            }
        });
        DATA_WATCHERS.put(2, new DataWatcherObjectVarInt());
        DATA_WATCHERS.put(3, new ReadableDataWatcherObject<Float>() {
            public void readFromStream(ByteBuf byteBuf, ProtocolVersion protocolVersion, String s) throws DecoderException {
                value = byteBuf.readFloatLE();
            }

            public void writeToStream(ByteBuf byteBuf, ProtocolVersion protocolVersion, String s) {
                byteBuf.writeFloatLE(value);
            }
        });
        DATA_WATCHERS.put(4, new DataWatcherObjectString());
        DATA_WATCHERS.put(5, new DataWatcherObjectItemStack());
        DATA_WATCHERS.put(6, new DataWatcherObjectPosition());
        DATA_WATCHERS.put(7, new ReadableDataWatcherObject<Long>() {
            public void readFromStream(ByteBuf byteBuf, ProtocolVersion protocolVersion, String s) throws DecoderException {
                value = VarNumberSerializer.readSVarLong(byteBuf);
            }

            public void writeToStream(ByteBuf byteBuf, ProtocolVersion protocolVersion, String s) {
                VarNumberSerializer.writeSVarLong(byteBuf, value);
            }
        });
        DATA_WATCHERS.put(8, new DataWatcherObjectPosition());
    }

    public static final String META_KEY = "__HOLOGRAM_PACKET_LISTENER";

    public HologramsPacketListener(Connection con) {
        this.con = con;
        con.addMetadata(META_KEY, this);
    }

    public static HologramsPacketListener get(Connection con) {
        return (HologramsPacketListener) con.getMetadata(META_KEY);
    }

    public void clean() {
        cachedArmorStands.clear();
    }

    @Override
    public void onRawPacketSending(RawPacketEvent event) {
        ByteBuf data = event.getData();

        int packetId = VarNumberSerializer.readVarInt(data);

        data.readByte();
        data.readByte();

        if (packetId == PEPacketIDs.ENTITY_TELEPORT) {
            long entityId = VarNumberSerializer.readVarLong(data);

            if (!cachedArmorStands.containsKey(entityId))
                return;

            float x = data.readFloatLE();
            float y = data.readFloatLE() + (float) Y_OFFSET;
            float z = data.readFloatLE();
            int pitch = data.readByte();
            int headYaw = data.readByte();
            int yaw = data.readByte();
            boolean onGround = data.readBoolean();
            event.setData(new PlayerMovePacket(entityId, x, y, z, pitch, headYaw, yaw, SetPosition.ANIMATION_MODE_ALL, onGround).encode(con));
            return;
        }
        if (packetId == PEPacketIDs.SPAWN_ENTITY) {
            VarNumberSerializer.readSVarLong(data);// unique id
            long entityId = VarNumberSerializer.readVarLong(data); // runtime id
            int typeId = VarNumberSerializer.readVarInt(data);

            if (cachedArmorStands.containsKey(entityId))
                return;

            if (typeId != ARMOR_STAND_ID)
                return;

            float x = data.readFloatLE();
            float y = data.readFloatLE();
            float z = data.readFloatLE();

            data.readFloatLE(); // motx
            data.readFloatLE(); // moty
            data.readFloatLE(); // motz

            data.readFloatLE(); // pitch
            data.readFloatLE(); // yaw

            {
                int len = VarNumberSerializer.readVarInt(data);// attribute length, unused
                for (int i = 0; i < len; i++) {
                    StringSerializer.readString(data, ProtocolVersion.MINECRAFT_PE);
                    data.readFloatLE();
                    data.readFloatLE();
                    data.readFloatLE();
                }
            }

            CachedArmorStand armorStand = new CachedArmorStand(x, y, z);
            cachedArmorStands.put(entityId, armorStand);

            String hologramName = retriveHologramName(data);

            if (hologramName == null)
                return;

            event.setCancelled(true);

            armorStand.nametag = hologramName;
            armorStand.setHologram(true);

            // omg it is an hologram :O
            armorStand.spawnHologram(con.getNetworkDataCache().getWatchedEntity((int) entityId), this);
            return;
        }
        if (packetId == PEPacketIDs.SET_ENTITY_DATA) {
            long entityId = VarNumberSerializer.readVarLong(data);

            if (!cachedArmorStands.containsKey(entityId))
                return;

            String hologramName = retriveHologramName(data);

            if (hologramName == null)
                return;

            // omg it is an hologram :O
            CachedArmorStand armorStand = cachedArmorStands.get(entityId);

            armorStand.setHologram(true);

            if (armorStand.spawned)
                return;

            // Kill current armor stand
            event.setData(new EntityDestroyPacket(entityId).encode(con));

            armorStand.nametag = hologramName;
            armorStand.setHologram(true);

            cachedArmorStands.put(entityId, armorStand);


            armorStand.spawnHologram(con.getNetworkDataCache().getWatchedEntity((int) entityId), this);
            return;
        }
        if (packetId == PEPacketIDs.ENTITY_DESTROY) {
            long entityId = VarNumberSerializer.readSVarLong(data);
            cachedArmorStands.remove(entityId);
            return;
        }
    }

    public String retriveHologramName(ByteBuf data) {
        boolean hasCustomName = false;
        boolean invisible = false;
        boolean shownametag = false;
        //boolean alwaysShowNametag = false;
        //boolean showBase = false;
        String nametag = null;

        int length = VarNumberSerializer.readVarInt(data);

        for (int idx = 0; length > idx; idx++) {
            int metaKey = VarNumberSerializer.readVarInt(data);
            int metaType = VarNumberSerializer.readVarInt(data) % 8;

            System.out.println("!!! meta type " + metaType);
            System.out.println("!!! meta key " + metaKey);

            ReadableDataWatcherObject<?> dw = DATA_WATCHERS.get(metaType);

            dw.readFromStream(data, con.getVersion(), I18NData.DEFAULT_LOCALE);

            if (metaKey == 4) {
                nametag = (String) dw.getValue();
                hasCustomName = !nametag.isEmpty();
            }

            if (metaKey == 0) {
                long peBaseFlags = ((Number) dw.getValue()).longValue();
                System.out.println("!!! meta ctx " + Long.toBinaryString(peBaseFlags));
                invisible = ((peBaseFlags >> (EntityMetadata.PeMetaBase.FLAG_INVISIBLE - 1)) & 1) == 1;
                shownametag = ((peBaseFlags >> (EntityMetadata.PeMetaBase.FLAG_SHOW_NAMETAG - 1)) & 1) == 1;
                System.out.println(String.format("!!! invisible=%s shownametag=%s", invisible, shownametag));
            }
        }

        return hasCustomName && invisible && shownametag ? nametag : null;
    }

    static class CachedArmorStand {
        private float x;
        private float y;
        private float z;
        private boolean spawned = false;
        private String nametag;
        private boolean hologram = false;

        public CachedArmorStand(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public void spawnHologram(NetworkEntity entity, HologramsPacketListener listener) {
            spawned = true;

            CollectionsUtils.ArrayMap<DataWatcherObject<?>> metadata = new CollectionsUtils.ArrayMap<>(EntityMetadata.PeMetaBase.BOUNDINGBOX_HEIGTH + 1);
            long peBaseFlags = entity.getDataCache().getPeBaseFlags();
            System.out.println("!!! sent new meta ctx " + Long.toBinaryString(peBaseFlags));
//            metadata.put(EntityMetadata.PeMetaBase.FLAGS, new DataWatcherObjectSVarLong(peBaseFlags));
            metadata.put(EntityMetadata.PeMetaBase.NAMETAG, new DataWatcherObjectString(nametag));
			metadata.put(EntityMetadata.PeMetaBase.SCALE, new DataWatcherObjectFloatLe(0.001f)); // scale
			metadata.put(EntityMetadata.PeMetaBase.BOUNDINGBOX_WIDTH, new DataWatcherObjectFloatLe(0.001f)); // bb width
			metadata.put(EntityMetadata.PeMetaBase.BOUNDINGBOX_HEIGTH, new DataWatcherObjectFloatLe(0.001f)); // bb height

            SpawnPlayerPacket packet = new SpawnPlayerPacket(
                    UUID.randomUUID(),
                    nametag,
                    entity.getId(),
                    x, y, z, // coordinates
                    0, 0, 0, // motion
                    0, 0, 0, // pitch, head yaw & yaw
                    metadata
            );

            PocketCon.sendPocketPacket(listener.con, packet);
        }

        public boolean isHologram() {
            return hologram;
        }

        public void setHologram(boolean isHologram) {
            this.hologram = isHologram;
        }
    }
}
