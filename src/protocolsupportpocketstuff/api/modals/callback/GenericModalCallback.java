package protocolsupportpocketstuff.api.modals.callback;

import org.bukkit.entity.Player;
import protocolsupportpocketstuff.api.modals.ModalType;

public abstract class GenericModalCallback extends ModalCallback {

    @Override
    public ModalType getModalType() {
        return ModalType.GENERIC_MODAL;
    }

    public abstract void onGenericModalResponse(Player player, String modalJSON, boolean closedByClient);
}
