package protocolsupportpocketstuff.api.event;

import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.event.HandlerList;
import protocolsupport.api.Connection;
import protocolsupport.api.events.ConnectionEvent;
import protocolsupportpocketstuff.api.modals.Modal;
import protocolsupportpocketstuff.api.modals.callback.ModalCallback;

public class ServerSettingsRequestEvent extends ConnectionEvent {

    public static final HandlerList HANDLER_LIST = new HandlerList();

    protected Pair<Modal, ModalCallback> settings;

    public ServerSettingsRequestEvent(Connection connection) {
        super(connection);
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    public Pair<Modal, ModalCallback> getSettings() {
        return settings;
    }

    public void setSettings(Pair<Modal, ModalCallback> settings) {
        this.settings = settings;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }
}
