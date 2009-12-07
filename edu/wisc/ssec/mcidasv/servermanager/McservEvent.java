package edu.wisc.ssec.mcidasv.servermanager;

import java.util.EventObject;

import edu.wisc.ssec.mcidasv.util.Contract;

public class McservEvent extends EventObject {

    public enum McservStatus { STARTED, STOPPED, DIED, RESTARTED, NOT_STARTED, NO_STATUS };

    public enum EventLevel { ERROR, NORMAL, DEBUG };

    private final McservStatus status;

    private final EventLevel level;

    private final String msg;

    public McservEvent(final EntryStore source, final McservStatus status, final EventLevel level, final String msg) {
        super(source);

        Contract.notNull(source, "McservEvents cannot originate from a null source object");
        Contract.notNull(status, "McservEvents must have a McservStatus");
        Contract.notNull(level, "McservEvents must have an EventLevel");
        Contract.notNull(msg, "McservEvents must have a message");

        this.status = status;
        this.level = level;
        this.msg = msg;
    }

    public McservStatus getStatus() {
        return status;
    }

    public EventLevel getLevel() {
        return level;
    }

    public String getMsg() {
        return msg;
    }

    @Override public String toString() {
        return String.format("[McservEvent@%x: status=%s, level=%s, msg=%s]", hashCode(), status, level, msg);
    }
}
