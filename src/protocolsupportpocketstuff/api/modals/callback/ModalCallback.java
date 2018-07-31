package protocolsupportpocketstuff.api.modals.callback;

import org.bukkit.entity.Player;
import protocolsupportpocketstuff.api.modals.ModalType;

public abstract class ModalCallback {
	public abstract void onModalResponse(Player player, String modalJSON, boolean isClosedByClient);

    public abstract ModalType getModalType();
}
