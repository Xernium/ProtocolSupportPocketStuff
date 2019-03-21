package protocolsupportpocketstuff.api.modals.callback;

import org.bukkit.entity.Player;
import protocolsupport.libs.com.google.gson.JsonArray;
import protocolsupportpocketstuff.api.modals.ModalType;

public abstract class ComplexFormCallback extends ModalCallback {
	public abstract void onComplexFormResponse(Player player, String modalJSON, boolean isClosedByClient, JsonArray jsonArray);

    @Override
    public ModalType getModalType() {
        return ModalType.COMPLEX_FORM;
    }
}
