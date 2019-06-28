package protocolsupportpocketstuff.zplatform;

import org.bukkit.entity.Player;

import protocolsupport.api.ServerPlatformIdentifier;
import protocolsupportpocketstuff.api.skins.SkinUtils.SkinDataWrapper;
import protocolsupportpocketstuff.zplatform.impl.spigot.SpigotStuff;

public abstract class PlatformThings {

	public abstract void setSkinProperties(Player player, SkinDataWrapper skindata);

	public abstract void sendPlayerSkin(Player player, SkinDataWrapper skindata);

	private static PlatformThings stuff;

	public static void bakeStuff() {
		switch(ServerPlatformIdentifier.get()) {
		case SPIGOT:
			stuff = new SpigotStuff();
			break;
		case GLOWSTONE:
//			stuff = new GlowstoneStuff();
//			break;
		default:
			throw new RuntimeException("Unsupported server platform!");
		}
	}

	public static PlatformThings getStuff() {
		return stuff;
	}

}
