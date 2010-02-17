/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2010
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * http://www.ssec.wisc.edu/mcidas
 * 
 * All Rights Reserved
 * 
 * McIDAS-V is built on Unidata's IDV and SSEC's VisAD libraries, and
 * some McIDAS-V source code is based on IDV and VisAD source code.  
 * 
 * McIDAS-V is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * McIDAS-V is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */
package edu.wisc.ssec.mcidasv.servermanager;

import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.arrList;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;

import org.bushe.swing.event.EventBus;

import ucar.unidata.ui.CheckboxCategoryPanel;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.xml.PreferenceManager;
import ucar.unidata.xml.XmlObjectStore;

import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.McIDASV;
import edu.wisc.ssec.mcidasv.McIdasPreferenceManager;
import edu.wisc.ssec.mcidasv.servermanager.AddeEntry.EntryStatus;
import edu.wisc.ssec.mcidasv.servermanager.AddeEntry.EntryType;

// the preference "view"... doesn't allow adding or deleting, though does
// allow visibility toggling (and probably reordering...)
public class AddePreferences {

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

    /**
     * Property ID that allows McIDAS-V to remember whether or not the user
     * has chosen to use all available ADDE servers or has specified the 
     * {@literal "active"} servers.
     */
    private static final String PREF_LIST_SPECIFY = "mcv.servers.pref.specify";

    // TODO need to get open/close methods added to CheckboxCategoryPanel
//    private static final String PREF_LIST_TYPE_PREFIX = "mcv.servers.types.list";

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
    public AddePreferences(final EntryStore entryStore) {
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

        Map<EntryType, Set<AddeEntry>> entries = 
            entryStore.getVerifiedEntriesByTypes();

        final Map<AddeEntry, JCheckBox> entryToggles = 
            new LinkedHashMap<AddeEntry, JCheckBox>();

        final List<CheckboxCategoryPanel> typePanels = arrList();
        List<JPanel> compList = arrList();

        // create checkboxes for each AddeEntry and add 'em to the appropriate 
        // CheckboxCategoryPanel 
        for (EntryType type : EntryType.values()) {
            final Set<AddeEntry> subset = entries.get(type);
            Set<String> observedEntries = new HashSet<String>(subset.size());
            final CheckboxCategoryPanel typePanel = 
                new CheckboxCategoryPanel(type.toString(), false);

            for (AddeEntry entry : subset) {
                String entryText = entry.getEntryText();
                if (observedEntries.contains(entryText))
                    continue;

                boolean enabled = (entry.getEntryStatus() == EntryStatus.ENABLED);
                JCheckBox cbx = new JCheckBox(entryText, enabled);
                entryToggles.put(entry, cbx);
                typePanel.addItem(cbx);
                typePanel.add(GuiUtils.inset(cbx, new Insets(0, 20, 0, 0)));
                observedEntries.add(entryText);
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

        // user wants to add a server! make it so.
        final JButton addServer = new JButton("Add ADDE Servers...");
        addServer.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
//                System.err.println("LOOK AT ME");
            }
        });

        // import list of servers
        final JButton importServers = new JButton("Import Servers...");
        importServers.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
//                System.err.println("LOOK AT ME");
            }
        });

        boolean useAll = false;
        boolean specify = false;
        if (getSpecifyServers().equals("ALL")) {
            useAll = true;
        } else {
            specify = true;
        }

        // disable user selection of entries--they're using everything
        final JRadioButton useAllBtn = 
            new JRadioButton("Use all ADDE entries", useAll);
        useAllBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                GuiUtils.enableTree(cbPanel, !useAllBtn.isSelected());
                GuiUtils.enableTree(allOn, !useAllBtn.isSelected());
                GuiUtils.enableTree(allOff, !useAllBtn.isSelected());
                setSpecifyServers("ALL");

            }
        });

        // let the user specify the "active" set, enable entry selection
        final JRadioButton useTheseBtn = 
            new JRadioButton("Use selected ADDE entries:", specify);
        useTheseBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                GuiUtils.enableTree(cbPanel, !useAllBtn.isSelected());
                GuiUtils.enableTree(allOn, !useAllBtn.isSelected());
                GuiUtils.enableTree(allOff, !useAllBtn.isSelected());
                setSpecifyServers("SPECIFY");
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
                        GuiUtils.top(GuiUtils.vbox(allOn, allOff, addServer, importServers)),
                        4), cbScroller));

        JPanel entryPanel =
            GuiUtils.topCenter(
                GuiUtils.inset(
                    new JLabel("Specify the active ADDE servers:"),
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
                Map<AddeEntry, JCheckBox> toggles = 
                    (Map<AddeEntry, JCheckBox>)data;

                boolean updated = false;
                for (Entry<AddeEntry, JCheckBox> entry : 
                    toggles.entrySet()) 
                {
                    AddeEntry e = entry.getKey();
                    JCheckBox c = entry.getValue();

                    EntryStatus currentStatus = e.getEntryStatus();
                    EntryStatus nextStatus = (c.isSelected()) ? EntryStatus.ENABLED : EntryStatus.DISABLED;

                    if (currentStatus != nextStatus)
                        updated = true;
                    e.setEntryStatus(nextStatus);
                }

                if (updated) {
                    EventBus.publish(EntryStore.Event.UPDATE);
                }
            }
        };

        // add the panel, listeners, and so on to the preference manager.
        prefManager.add(Constants.PREF_LIST_ADDE_SERVERS, "blah", 
            entryListener, entryPanel, entryToggles);
    }

    private void setSpecifyServers(final String value) {
        McIDASV mcv = McIDASV.getStaticMcv();
        if (mcv == null)
            return;
        mcv.getStore().put(PREF_LIST_SPECIFY, value);
    }

    private String getSpecifyServers() {
        McIDASV mcv = McIDASV.getStaticMcv();
        if (mcv == null)
            return "ALL";
        return mcv.getStore().get(PREF_LIST_SPECIFY, "ALL");
    }

    // getTopPanel seems to break CheckboxCategoryPanel
    private void setCategoryPanelIcon(final CheckboxCategoryPanel panel, final Icon newIcon) {
        JPanel topPanel = panel.getTopPanel();
//        System.err.println("comp count: "+topPanel.getComponentCount());
        for (int i = 0; i < topPanel.getComponentCount(); i++) {
//            System.err.println("comp idx="+i+" comp="+topPanel.getComponent(i));
        }
    }
}
