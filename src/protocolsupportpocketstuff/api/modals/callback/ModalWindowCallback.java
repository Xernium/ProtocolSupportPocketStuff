package protocolsupportpocketstuff.api.modals.callback;

import org.bukkit.entity.Player;
import protocolsupportpocketstuff.api.modals.ModalType;

public abstract class ModalWindowCallback extends ModalCallback {
	public abstract void onModalWindowResponse(Player player, String modalJSON, boolean isClosedByClient, boolean result);

    @Override
    public ModalType getModalType() {
        return ModalType.MODAL_WINDOW;
    }
}
