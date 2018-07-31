package protocolsupportpocketstuff.api.modals.callback;

import org.bukkit.entity.Player;
import protocolsupportpocketstuff.api.modals.ModalType;

public abstract class SimpleFormCallback extends ModalCallback {
	public abstract void onSimpleFormResponse(Player player, String modalJSON, boolean isClosedByClient, int clickedButton);

	@Override
	public void onModalResponse(Player player, String modalJSON, boolean isClosedByClient) {
	}

    @Override
    public ModalType getModalType() {
        return ModalType.SIMPLE_FORM;
    }
}
