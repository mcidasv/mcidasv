package edu.wisc.ssec.mcidasv.servermanager;

import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.arrList;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;

import ucar.unidata.ui.CheckboxCategoryPanel;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.xml.PreferenceManager;
import ucar.unidata.xml.XmlObjectStore;

import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.McIdasPreferenceManager;
import edu.wisc.ssec.mcidasv.servermanager.RemoteAddeEntry.EntryStatus;
import edu.wisc.ssec.mcidasv.servermanager.RemoteAddeEntry.EntryType;

// the preference "view"... doesn't allow adding or deleting, though does
// allow visibility toggling (and probably reordering...)
public class RemoteAddePreferences {

    /** 
     * Property ID for controlling the display of {@literal "site"} servers 
     * in the server preferences. 
     */
    private static final String PREF_LIST_SITE_SERV = "mcv.servers.listsite";

    /** 
     * Property ID for controlling the display of {@literal "default mcv"} 
     * servers in the server preferences. 
     */
    private static final String PREF_LIST_DEFAULT_SERV = "mcv.servers.listdefault";

    /** 
     * Property ID for controlling the display of {@literal "MCTABLE"} 
     * servers in the server preferences. 
     */
    private static final String PREF_LIST_MCTABLE_SERV = "mcv.servers.listmcx";

    /** 
     * Property ID for controlling the display of {@literal "user"} servers 
     * in the server preferences. 
     */
    private static final String PREF_LIST_USER_SERV = "mcv.servers.listuser";

    /** Contains the lists of ADDE servers that we'll use as content. */
    private final EntryStore entryStore;

    /**
     * Prepares a new preference panel based upon the supplied 
     * {@link EntryStore}.
     * 
     * @param entryStore The {@code EntryStore} to query. Cannot be 
     * {@code null}.
     * 
     * @throws NullPointerException if {@code entryStore} is {@code null}.
     */
    public RemoteAddePreferences(final EntryStore entryStore) {
        if (entryStore == null)
            throw new NullPointerException("EntryStore cannot be null");

        this.entryStore = entryStore;
    }

    /**
     * Builds the remote server preference panel, using the given 
     * {@link McIdasPreferenceManager}.
     * 
     * @param prefManager The {@code McIdasPreferenceManager} that will 
     * contain this preference panel. Cannot be {@code null}.
     * 
     * @throws NullPointerException if {@code prefManager} is {@code null}.
     */
    public void buildPanel(McIdasPreferenceManager prefManager) {
        if (prefManager == null)
            throw new NullPointerException("Pref Manager cannot be null");

        Map<EntryType, Set<RemoteAddeEntry>> entries = 
            entryStore.getVerifiedEntriesByTypes();

        final Map<RemoteAddeEntry, JCheckBox> entryToggles = 
            new LinkedHashMap<RemoteAddeEntry, JCheckBox>();

        final List<CheckboxCategoryPanel> typePanels = arrList();
        List<JPanel> compList = arrList();

        // create checkboxes for each RemoteAddeEntry and add 'em to the 
        // appropriate CheckboxCategoryPanel 
        for (EntryType type : EntryType.values()) {
            final Set<RemoteAddeEntry> subset = entries.get(type);
            final CheckboxCategoryPanel typePanel = 
                new CheckboxCategoryPanel(type.toString(), false);

            for (RemoteAddeEntry entry : subset) {
                JCheckBox cbx = new JCheckBox(entry.getEntryText(), true);
                entryToggles.put(entry, cbx);
                typePanel.addItem(cbx);
                typePanel.add(GuiUtils.inset(cbx, new Insets(0, 20, 0, 0)));
            }

            compList.add(typePanel.getTopPanel());
            compList.add(typePanel);
            typePanels.add(typePanel);
            typePanel.checkVisCbx();
        }

        // create the basic pref panel
        // TODO(jon): determine dimensions more intelligently!
        final JPanel cbPanel = GuiUtils.top(GuiUtils.vbox(compList));
        JScrollPane cbScroller = new JScrollPane(cbPanel);
        cbScroller.getVerticalScrollBar().setUnitIncrement(10);
        cbScroller.setPreferredSize(new Dimension(300, 300));

        // handle the user opting to enable all servers
        final JButton allOn = new JButton("All on");
        allOn.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                for (CheckboxCategoryPanel cbcp : typePanels)
                    cbcp.toggleAll(true);
            }
        });

        // handle the user opting to disable all servers
        final JButton allOff = new JButton("All off");
        allOff.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                for (CheckboxCategoryPanel cbcp : typePanels)
                    cbcp.toggleAll(false);
            }
        });

        // disable user selection of entries--they're using everything
        final JRadioButton useAllBtn = 
            new JRadioButton("Use all ADDE entries", true);
        useAllBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                GuiUtils.enableTree(cbPanel, !useAllBtn.isSelected());
                GuiUtils.enableTree(allOn, !useAllBtn.isSelected());
                GuiUtils.enableTree(allOff, !useAllBtn.isSelected());

            }
        });

        // let the user specify the "active" set, enable entry selection
        final JRadioButton useTheseBtn = 
            new JRadioButton("Use selected ADDE entries:", false);
        useTheseBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                GuiUtils.enableTree(cbPanel, !useAllBtn.isSelected());
                GuiUtils.enableTree(allOn, !useAllBtn.isSelected());
                GuiUtils.enableTree(allOff, !useAllBtn.isSelected());
            }
        });
        GuiUtils.buttonGroup(useAllBtn, useTheseBtn);

        // force the selection state
        // TODO(jon): extract this out into its own method.
        GuiUtils.enableTree(cbPanel, !useAllBtn.isSelected());
        GuiUtils.enableTree(allOn, !useAllBtn.isSelected());
        GuiUtils.enableTree(allOff, !useAllBtn.isSelected());

        JPanel widgetPanel =
            GuiUtils.topCenter(
                GuiUtils.hbox(useAllBtn, useTheseBtn),
                GuiUtils.leftCenter(
                    GuiUtils.inset(
                        GuiUtils.top(GuiUtils.vbox(allOn, allOff)),
                        4), cbScroller));

        JPanel entryPanel =
            GuiUtils.topCenter(
                GuiUtils.inset(
                    new JLabel("Specify the active remote ADDE servers:"),
                    4), widgetPanel);
        entryPanel = GuiUtils.inset(GuiUtils.left(entryPanel), 6);

        // iterate through all the entries and "apply" any changes: 
        // reordering, removing, visibility changes, etc
        PreferenceManager entryListener = new PreferenceManager() {
            public void applyPreference(XmlObjectStore store, Object data) {

                // this won't break because the data parameter is whatever
                // has been passed in to "prefManager.add(...)". in this case,
                // it's the "entryToggles" variable.
                @SuppressWarnings("unchecked")
                Map<RemoteAddeEntry, JCheckBox> toggles = 
                    (Map<RemoteAddeEntry, JCheckBox>)data;

                for (Entry<RemoteAddeEntry, JCheckBox> entry : 
                    toggles.entrySet()) 
                {
                    RemoteAddeEntry e = entry.getKey();
                    JCheckBox c = entry.getValue();
                    if (!c.isSelected())
                        e.setEntryStatus(EntryStatus.DISABLED);
                }
            }
        };

        // add the panel, listeners, and so on to the preference manager.
        prefManager.add(Constants.PREF_LIST_ADDE_SERVERS, "blah", 
            entryListener, entryPanel, entryToggles);
    }
}
