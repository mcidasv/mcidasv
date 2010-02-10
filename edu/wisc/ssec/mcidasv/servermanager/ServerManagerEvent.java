package edu.wisc.ssec.mcidasv.servermanager;

public class ServerManagerEvent {

    public static final ServerManagerEvent Added = new ServerManagerEvent(Action.ADDITION);
    public static final ServerManagerEvent Removed = new ServerManagerEvent(Action.REMOVAL);
    public static final ServerManagerEvent Replaced = new ServerManagerEvent(Action.REPLACEMENT);
    public static final ServerManagerEvent Failed = new ServerManagerEvent(Action.FAILURE);
    public static final ServerManagerEvent Updated = new ServerManagerEvent(Action.UPDATE);
    public static final ServerManagerEvent Unknown = new ServerManagerEvent(Action.UNKNOWN);
    public static final ServerManagerEvent Started = new ServerManagerEvent(Action.STARTED);

    public enum Action { REPLACEMENT, REMOVAL, ADDITION, UPDATE, FAILURE, STARTED, UNKNOWN };

    private final Action action;

    public ServerManagerEvent(final Action action) {
        this.action = action;
    }

    public Action getAction() {
        return action;
    }
    
    public String toString() {
        return String.format("[ServerManagerEvent@%x: action=%s]", hashCode(), action);
    }
}
