package protocolsupportpocketstuff.util;

import com.google.common.collect.ImmutableList;
import io.netty.buffer.ByteBuf;
import lombok.Cleanup;
import net.minecraft.server.v1_12_R1.PacketPlayInUseEntity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import protocolsupport.api.Connection;
import protocolsupport.api.ProtocolSupportAPI;
import protocolsupport.api.ProtocolVersion;
import protocolsupport.protocol.packet.middleimpl.ClientBoundPacketData;
import protocolsupport.protocol.packet.middleimpl.clientbound.play.v_pe.EntityMetadata;
import protocolsupport.protocol.packet.middleimpl.clientbound.play.v_pe_1_5.EntityMoveAbsolute;
import protocolsupport.protocol.pipeline.version.v_pe.PEPacketEncoder;
import protocolsupport.protocol.serializer.MiscSerializer;
import protocolsupport.protocol.typeremapper.pe.PEPacketIDs;
import protocolsupport.protocol.utils.datawatcher.DataWatcherObject;
import protocolsupport.protocol.utils.datawatcher.objects.DataWatcherObjectFloatLe;
import protocolsupport.protocol.utils.datawatcher.objects.DataWatcherObjectSVarLong;
import protocolsupport.protocol.utils.datawatcher.objects.DataWatcherObjectString;
import protocolsupport.protocol.utils.types.networkentity.NetworkEntity;
import protocolsupport.utils.CollectionsUtils;
import protocolsupport.utils.netty.Allocator;
import protocolsupportpocketstuff.ProtocolSupportPocketStuff;
import protocolsupportpocketstuff.api.util.PocketPlayer;
import protocolsupportpocketstuff.packet.play.EntityDataPacket;
import protocolsupportpocketstuff.packet.play.EntityDestroyPacket;
import protocolsupportpocketstuff.packet.play.SpawnEntityPacket;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ActionButton implements Listener {

    private static final NetworkEntity BUTTON = NetworkEntity.createMob(UUID.randomUUID(), 193164, 37);
    private Map<UUID, TransactionListener> buttonOpened = new HashMap<>();

    public void sendButton(Player player, String buttonText, int displayTick, Runnable click) {
        setButtonAndText(player, buttonText);
        if (buttonOpened.isEmpty()) {
            Bukkit.getPluginManager().registerEvents(this, ProtocolSupportPocketStuff.getInstance());
        }
        register(player, click);
        Bukkit.getScheduler().runTaskLater(ProtocolSupportPocketStuff.getInstance(), () -> closeButton(player), displayTick);
    }

    private void register(Player player, Runnable click) {
        if (buttonOpened.containsKey(player.getUniqueId())) {
            buttonOpened.get(player.getUniqueId()).command = click;
        } else {
            TransactionListener listener = new TransactionListener(click);
            buttonOpened.put(player.getUniqueId(), listener);
            ProtocolSupportAPI.getConnection(player).addPacketListener(listener);
        }
    }

    public void sendButton(Player player, String buttonText, Runnable click) {
        setButtonAndText(player, buttonText);
        if (buttonOpened.isEmpty()) {
            Bukkit.getPluginManager().registerEvents(this, ProtocolSupportPocketStuff.getInstance());
        }
        register(player, click);
    }

    public boolean hasButton(Player player) {
        return buttonOpened.containsKey(player.getUniqueId());
    }

    public void closeButton(Player player) {
        changeText(player, "");
        EntityDestroyPacket packet = new EntityDestroyPacket(BUTTON.getId());
        PocketPlayer.sendPocketPacket(player, packet);
        TransactionListener listener = buttonOpened.remove(player.getUniqueId());
        ProtocolSupportAPI.getConnection(player).removePacketListener(listener);
        if (buttonOpened.isEmpty()) {
            HandlerList.unregisterAll(this);
        }
    }

    private void setButtonAndText(Player player, String value) {
        if (!buttonOpened.containsKey(player.getUniqueId())) {
            spawnEntity(player);
        }

        changeText(player, value);
    }

    public void changeText(Player player, String value) {
        CollectionsUtils.ArrayMap<DataWatcherObject<?>> metadata = new CollectionsUtils.ArrayMap<>(EntityMetadata.PeMetaBase.BUTTON_TEXT + 1);
        metadata.put(EntityMetadata.PeMetaBase.BUTTON_TEXT, new DataWatcherObjectString(value));

        EntityDataPacket packet = new EntityDataPacket(Integer.MAX_VALUE, metadata);
        PocketPlayer.sendPocketPacket(player, packet);
    }

    private void spawnEntity(Player player) {
        CollectionsUtils.ArrayMap<DataWatcherObject<?>> metadata = new CollectionsUtils.ArrayMap<>(EntityMetadata.PeMetaBase.SCALE + 1);
        metadata.put(EntityMetadata.PeMetaBase.FLAGS, new DataWatcherObjectSVarLong(1 << 5));
        metadata.put(EntityMetadata.PeMetaBase.NAMETAG, new DataWatcherObjectString(""));
        metadata.put(EntityMetadata.PeMetaBase.SCALE, new DataWatcherObjectFloatLe(5));

        Location location = player.getLocation();

        SpawnEntityPacket packet = new SpawnEntityPacket(BUTTON.getId(), 37,
                (float) location.getX(), (float) location.getY() + 1, (float) location.getZ(),
                0F, 0F, 0F,
                location.getPitch(), location.getYaw(), ImmutableList.of(),
                metadata);

        PocketPlayer.sendPocketPacket(player, packet);
    }

    @EventHandler
    public void event(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (buttonOpened.containsKey(event.getPlayer().getUniqueId())) {
            Location location = player.getLocation();

            @Cleanup
            ClientBoundPacketData data = EntityMoveAbsolute.create(ProtocolVersion.MINECRAFT_PE_1_5, BUTTON, location.getX(), location.getY(), location.getZ(), Byte.MAX_VALUE, Byte.MAX_VALUE, Byte.MAX_VALUE, false, false);
            @Cleanup("release")
            ByteBuf buf = Allocator.allocateBuffer();
            PEPacketEncoder.sWritePacketId(ProtocolSupportAPI.getProtocolVersion(player), buf, PEPacketIDs.MOVE_ENTITY_ABSOLUTE);
            buf.writeBytes(data);
            byte[] bytes = MiscSerializer.readAllBytes(buf);
            ProtocolSupportAPI.getConnection(player).sendRawPacket(bytes);
        }
    }

    @EventHandler
    public void event(PlayerQuitEvent event) {
        buttonOpened.remove(event.getPlayer().getUniqueId());
        if (buttonOpened.isEmpty()) {
            HandlerList.unregisterAll(this);
        }
    }

    private static class TransactionListener extends Connection.PacketListener {

        private static Field entityIdFiled;

        static {
            try {
                entityIdFiled = PacketPlayInUseEntity.class.getDeclaredField("a");
                entityIdFiled.setAccessible(true);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }

        private Runnable command;

        public TransactionListener(Runnable command) {
            this.command = command;
        }

        @Override
        public void onPacketReceiving(PacketEvent event) {
            Object obj = event.getPacket();
            if (obj instanceof PacketPlayInUseEntity) {
                PacketPlayInUseEntity packet = (PacketPlayInUseEntity) obj;
                try {
                    int i = entityIdFiled.getInt(packet);
                    if (i == BUTTON.getId()) {
                        if (packet.a() == PacketPlayInUseEntity.EnumEntityUseAction.INTERACT) {
                            command.run();
                        }
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
