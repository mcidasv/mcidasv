package edu.wisc.ssec.mcidasv.servermanager;

import javax.swing.JDialog;
import javax.swing.JFrame;

import edu.wisc.ssec.mcidasv.McIDASV;

// this is accessible via the tools menu or some such thing and allows complete
// control over the available entries.
public class RemoteAddeManager {

    private final McIDASV mcv;
    private final EntryStore entryStore;

    public RemoteAddeManager(final McIDASV mcv, final EntryStore entryStore) {
        this.mcv = mcv;
        this.entryStore = entryStore;
    }

    public void showManager() {
        
    }

    public void showAddEntryDialog() {
        JDialog dialog = new JDialog((JFrame)null, "Add New ADDE Server", true);
        AddRemoteAddeEntry entryPanel = new AddRemoteAddeEntry();
        dialog.setContentPane(entryPanel);
        dialog.pack();
        dialog.setResizable(false);
        dialog.setVisible(true);
    }
}
