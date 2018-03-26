package protocolsupportpocketstuff.util;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.server.v1_12_R1.EntityHuman;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.simple.JSONValue;
import protocolsupport.api.Connection;
import protocolsupport.api.ServerPlatformIdentifier;
import protocolsupportpocketstuff.ProtocolSupportPocketStuff;
import protocolsupportpocketstuff.api.util.SkinUtils;
import protocolsupportpocketstuff.libs.kevinsawicki.http.HttpRequest;
import protocolsupportpocketstuff.storage.Skins;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MineskinThread implements Runnable {
	ProtocolSupportPocketStuff plugin;
	Connection connection;
	String uniqueSkinId;
	BufferedImage skin;
	boolean isSlim;

	public MineskinThread(ProtocolSupportPocketStuff plugin, Connection connection, String uniqueSkinId, byte[] skinByteArray, boolean isSlim) {
		this.plugin = plugin;
		this.connection = connection;
		this.uniqueSkinId = uniqueSkinId;
		this.skin = SkinUtils.fromData(skinByteArray);
		this.isSlim = isSlim;
	}

	public MineskinThread(ProtocolSupportPocketStuff plugin, Connection connection, String uniqueSkinId, BufferedImage skin, boolean isSlim) {
		this.plugin = plugin;
		this.connection = connection;
		this.uniqueSkinId = uniqueSkinId;
		this.skin = skin;
		this.isSlim = isSlim;
	}

	@Override
	public void run() {
		plugin.debug("Sending skin " + uniqueSkinId + " to MineSkin...");

		try {
			int tries = 0;
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			ImageIO.write(skin, "png", os);
			Map<String, ?> mineskinResponse = sendToMineSkin(os, isSlim);

			plugin.debug("[#" + (tries + 1) + "] " + mineskinResponse);

			while (mineskinResponse.containsKey("error")) {
				String error = String.valueOf(mineskinResponse.get("error"));
				if (!connection.isConnected()) {
					plugin.debug("[#" + (tries + 1) + "] Failed again... but the client disconnected, so we are going to ignore the skin!");
					return;
				}
				if (tries > 4) {
					plugin.pm("Failed to send skin to MineSkin after 5 tries (" + error + "), cancelling upload thread...");
					return;
				}
				plugin.debug("[#" + (tries + 1) + "] Failed to send skin! Retrying again in 5s...");
				Thread.sleep(5000); // Throttle
				mineskinResponse = sendToMineSkin(os, isSlim);
				plugin.debug("[#" + (tries + 1) + "] " + mineskinResponse);
				tries++;
			}

			Map<String, ?> skinData = (Map<String, ?>) mineskinResponse.get("data");
			Map<String, ?> skinTexture = (Map<String, ?>) skinData.get("texture");
			String signature = String.valueOf(skinTexture.get("signature"));
			String value = String.valueOf(skinTexture.get("value"));

			plugin.debug("Storing skin on cache...");
			Skins.INSTANCE.cachePcSkin(uniqueSkinId, new SkinUtils.SkinDataWrapper(value, signature, isSlim));
			hackyStuff(connection, value, signature);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static Map<String, ?> sendToMineSkin(ByteArrayOutputStream byteArrayOutputStream, boolean isSlim) throws IOException {
		InputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
		Map<String, ?> mineskinResponse = sendToMineSkin(inputStream, isSlim);
		inputStream.close();
		return mineskinResponse;
	}

	public static Map<String, ?> sendToMineSkin(InputStream inputStream, boolean isSlim) {
		HttpRequest httpRequest = HttpRequest.post("http://api.mineskin.org/generate/upload?name=&model=" + (isSlim ? "slim" : "steve") + "&visibility=1")
				.userAgent("ProtocolSupportPocketStuff");
		httpRequest.part("file", "mcpe_skin.png", null, inputStream);
		return (Map<String, ?>) JSONValue.parse(httpRequest.body());
	}

	enum Runner {
		INSTANCE;
		final ExecutorService service = Executors.newSingleThreadExecutor();
	}

	public void start() {
		Runner.INSTANCE.service.execute(this);
	}

	public void hackyStuff(Connection connection, String value, String signature) {
		// Wait until the player is logged in
		while (connection.getPlayer() == null) {
			if (!connection.isConnected()) {
				return;
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		plugin.debug("Player is logged in, applying skin...");

		Player player = connection.getPlayer();

		if (ProtocolSupportPocketStuff.platform == ServerPlatformIdentifier.SPIGOT) {
			CraftPlayer craftPlayer = ((CraftPlayer) player);
			EntityHuman entityHuman = craftPlayer.getHandle();

			try {
				Field gp2 = entityHuman.getClass().getSuperclass().getDeclaredField("g");
				gp2.setAccessible(true);
				GameProfile profile = (GameProfile) gp2.get(entityHuman);
				profile.getProperties().removeAll("textures");
				profile.getProperties().put("textures", new Property("textures", value, signature));
				gp2.set(entityHuman, profile);
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
		}

		//triggers an update for others player to see the new skin
		new BukkitRunnable() {
			public void run() {
				Bukkit.getOnlinePlayers().stream()
						.filter(onlinePlayer -> !onlinePlayer.equals(player))
						.filter(onlinePlayer -> onlinePlayer.canSee(player))
						.forEach(onlinePlayer -> {
							//removes the entity and display the new skin
							onlinePlayer.hidePlayer(player);
							onlinePlayer.showPlayer(player);
						});
			}
		}.runTask(ProtocolSupportPocketStuff.getInstance());
	}
}
