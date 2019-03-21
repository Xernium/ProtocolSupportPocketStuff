package protocolsupportpocketstuff.skin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import protocolsupport.api.Connection;
import protocolsupport.api.events.PlayerPropertiesResolveEvent;
import protocolsupportpocketstuff.ProtocolSupportPocketStuff;
import protocolsupportpocketstuff.api.event.PocketChangeSkinEvent;
import protocolsupportpocketstuff.api.util.PocketCon;
import protocolsupportpocketstuff.api.util.SkinUtils;
import protocolsupportpocketstuff.util.MineskinThread;
import protocolsupportpocketstuff.util.StuffUtils;

public class SkinListener implements Listener {
	
	private ProtocolSupportPocketStuff plugin;
	
	public SkinListener(ProtocolSupportPocketStuff plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void onPlayerPropertiesResolve(PlayerPropertiesResolveEvent e) {
		Connection con = e.getConnection();
		if (PocketCon.isPocketConnection(con)) {
			if (con.hasMetadata(StuffUtils.APPLY_SKIN_ON_JOIN_KEY)) {
				plugin.debug("Applying cached skin for " + e.getConnection() + "...");
				SkinUtils.SkinDataWrapper skinDataWrapper = (SkinUtils.SkinDataWrapper) con.getMetadata(StuffUtils.APPLY_SKIN_ON_JOIN_KEY);
				e.addProperty(new PlayerPropertiesResolveEvent.ProfileProperty(StuffUtils.SKIN_PROPERTY_NAME, skinDataWrapper.getValue(), skinDataWrapper.getSignature()));
				con.removeMetadata(StuffUtils.APPLY_SKIN_ON_JOIN_KEY);
			}
		}
	}

	@EventHandler
	public void onSkinChange(PocketChangeSkinEvent e) {
		plugin.debug(e.getPlayer().getName() + " changed skin in-game: Slim Skin? " + e.isSlim() + " Width: " + e.getSkin().getWidth());
		new MineskinThread(plugin, e.getConnection(), e.getUuid().toString(), e.getSkin(), e.isSlim()).start();
	}
}
