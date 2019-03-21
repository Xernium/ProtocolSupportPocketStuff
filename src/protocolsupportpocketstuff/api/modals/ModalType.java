package protocolsupportpocketstuff.api.modals;

import com.google.common.base.Preconditions;
import protocolsupportpocketstuff.api.modals.callback.ComplexFormCallback;
import protocolsupportpocketstuff.api.modals.callback.GenericModalCallback;
import protocolsupportpocketstuff.api.modals.callback.ModalCallback;
import protocolsupportpocketstuff.api.modals.callback.ModalWindowCallback;
import protocolsupportpocketstuff.api.modals.callback.SimpleFormCallback;

public enum ModalType {

	/**
	 * Use for generic modal callback only. NOT really modal type.
	 */
	GENERIC_MODAL("", GenericModalCallback.class),
	MODAL_WINDOW("modal", ModalWindowCallback.class), SIMPLE_FORM("form", SimpleFormCallback.class), COMPLEX_FORM("custom_form", ComplexFormCallback.class);

	private final String peName;
	private final Class<? extends ModalCallback> clazz;

	ModalType(String peName, Class<? extends ModalCallback> clazz) {
		this.peName = peName;
		this.clazz = clazz;
	}

	public String getPeName() {
		return peName;
	}

	public void requireType(ModalCallback callback) {
		Preconditions.checkState(GENERIC_MODAL.clazz.isInstance(callback) || clazz.isInstance(callback), "Mismatched modal & callback.");
	}

}
