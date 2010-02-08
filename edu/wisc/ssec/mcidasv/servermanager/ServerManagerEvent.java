package edu.wisc.ssec.mcidasv.servermanager;

public class ServerManagerEvent {

    public enum Action { REPLACEMENT, REMOVAL, ADDITION, FAILED, UNKNOWN };

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
