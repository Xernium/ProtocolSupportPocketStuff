package protocolsupportpocketstuff.util;

import com.google.common.collect.ImmutableList;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import protocolsupport.protocol.packet.middleimpl.clientbound.play.v_pe.EntityMetadata;
import protocolsupport.protocol.utils.datawatcher.DataWatcherObject;
import protocolsupport.protocol.utils.datawatcher.objects.DataWatcherObjectFloatLe;
import protocolsupport.protocol.utils.datawatcher.objects.DataWatcherObjectSVarLong;
import protocolsupport.protocol.utils.datawatcher.objects.DataWatcherObjectString;
import protocolsupport.utils.CollectionsUtils;
import protocolsupportpocketstuff.ProtocolSupportPocketStuff;
import protocolsupportpocketstuff.api.util.PocketPlayer;
import protocolsupportpocketstuff.packet.play.EntityDataPacket;
import protocolsupportpocketstuff.packet.play.EntityDestroyPacket;
import protocolsupportpocketstuff.packet.play.SpawnEntityPacket;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ActionButton implements Listener {

    private final long BUTTON_ID = 193164;
    private Map<UUID, Runnable> buttonOpened = new HashMap<>();

    public void sendButton(Player player, String buttonText, int displayTick, Runnable click) {
        setButtonAndText(player, buttonText);
        if (buttonOpened.isEmpty()) {
            Bukkit.getPluginManager().registerEvents(this, ProtocolSupportPocketStuff.getInstance());
        }
        buttonOpened.put(player.getUniqueId(), click);
        Bukkit.getScheduler().runTaskLater(ProtocolSupportPocketStuff.getInstance(), () -> closeButton(player), displayTick);
    }

    public void sendButton(Player player, String buttonText, Runnable click) {
        setButtonAndText(player, buttonText);
        if (buttonOpened.isEmpty()) {
            Bukkit.getPluginManager().registerEvents(this, ProtocolSupportPocketStuff.getInstance());
        }
        buttonOpened.put(player.getUniqueId(), click);
    }

    public boolean hasButton(Player player) {
        return buttonOpened.containsKey(player.getUniqueId());
    }

    public void closeButton(Player player) {
        changeText(player, "");
        EntityDestroyPacket packet = new EntityDestroyPacket(BUTTON_ID);
        PocketPlayer.sendPocketPacket(player, packet);
        buttonOpened.remove(player.getUniqueId());
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

    private void changeText(Player player, String value) {
        CollectionsUtils.ArrayMap<DataWatcherObject<?>> metadata = new CollectionsUtils.ArrayMap<>(EntityMetadata.PeMetaBase.BOUNDINGBOX_HEIGTH + 1);
        metadata.put(EntityMetadata.PeMetaBase.BUTTON_TEXT, new DataWatcherObjectString(value));

        EntityDataPacket packet = new EntityDataPacket(Integer.MAX_VALUE, metadata);
        PocketPlayer.sendPocketPacket(player, packet);
    }

    private void spawnEntity(Player player) {
        CollectionsUtils.ArrayMap<DataWatcherObject<?>> metadata = new CollectionsUtils.ArrayMap<>(EntityMetadata.PeMetaBase.BOUNDINGBOX_HEIGTH + 1);
        metadata.put(EntityMetadata.PeMetaBase.FLAGS, new DataWatcherObjectSVarLong(1 << 5));
        metadata.put(EntityMetadata.PeMetaBase.NAMETAG, new DataWatcherObjectString(""));
        metadata.put(EntityMetadata.PeMetaBase.SCALE, new DataWatcherObjectFloatLe(5));

        Location location = player.getLocation();

        SpawnEntityPacket packet = new SpawnEntityPacket(BUTTON_ID, 37,
                (float) location.getX(), (float) location.getY() + 1, (float) location.getZ(),
                0F, 0F, 0F,
                location.getPitch(), location.getYaw(), ImmutableList.of(),
                metadata);

        PocketPlayer.sendPocketPacket(player, packet);
    }

}
