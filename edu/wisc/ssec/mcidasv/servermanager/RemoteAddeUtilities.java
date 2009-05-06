package edu.wisc.ssec.mcidasv.servermanager;

import edu.wisc.ssec.mcidasv.servermanager.RemoteAddeEntry.EntryType;
import edu.wisc.ssec.mcidasv.servermanager.RemoteAddeEntry.EntryValidity;

// Note: this probably has some utility... don't delete quite yet!
public class RemoteAddeUtilities {

    // make an entry without accounting
    protected static RemoteAddeEntry _me(String h, String g, EntryType t, EntryValidity v) {
        return new RemoteAddeEntry.Builder(h, g).type(t).validity(v).build();
    }

    // make an entry with accounting
    protected static RemoteAddeEntry _meacc(String h, String g, EntryType t, String u, String p, EntryValidity v) {
        return new RemoteAddeEntry.Builder(h, g).type(t).account(u, p).validity(v).build();
    }
}
