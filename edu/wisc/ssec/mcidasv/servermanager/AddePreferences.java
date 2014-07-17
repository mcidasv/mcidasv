/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2014
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
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
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

import org.bushe.swing.event.EventBus;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventTopicSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.unidata.idv.IdvObjectStore;
import ucar.unidata.ui.CheckboxCategoryPanel;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LayoutUtil;
import ucar.unidata.xml.PreferenceManager;
import ucar.unidata.xml.XmlObjectStore;

import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.McIDASV;
import edu.wisc.ssec.mcidasv.McIdasPreferenceManager;
import edu.wisc.ssec.mcidasv.servermanager.AddeEntry.EntryStatus;
import edu.wisc.ssec.mcidasv.servermanager.AddeEntry.EntryType;
import edu.wisc.ssec.mcidasv.util.Contract;

/**
 * The ADDE Server preference panel is <b>almost</b> a read-only
 * {@literal "view"} of the current state of the server manager. The only
 * thing that users can change from here is the visibility of individual
 * {@link AddeEntry AddeEntries}, though there has been some talk of allowing
 * for reordering.
 */
public class AddePreferences {

    public enum Selection { ALL_ENTRIES, SPECIFIED_ENTRIES };

    private static final Logger logger = LoggerFactory.getLogger(AddePreferences.class);

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

    /** Panel that contains the various {@link AddeEntry}s. */
    private JPanel cbPanel = null;

    private JScrollPane cbScroller = null;

    /**
     * Allows the user to enable all {@link AddeEntry}s, <b><i>without</i></b> 
     * disabling the preference panel.
     */
    private JButton allOn = null;

    /**
     * Allows the user to disable all {@link AddeEntry}s, <b><i>without</i></b> 
     * disabling the preference panel.
     */
    private JButton allOff = null;

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
        AnnotationProcessor.process(this);
        if (entryStore == null) {
            throw new NullPointerException("EntryStore cannot be null");
        }
        this.entryStore = entryStore;
    }

    /**
     * Adds the various {@link AddePrefConglomeration} objects to the {@code prefManager}.
     * 
     * @param prefManager McIDAS-V's {@link PreferenceManager}. Should not be {@code null}.
     */
    public void addPanel(McIdasPreferenceManager prefManager) {
        AddePrefConglomeration notPretty = buildPanel((McIDASV)prefManager.getIdv());
        // add the panel, listeners, and so on to the preference manager.
        prefManager.add(notPretty.getName(), "blah", notPretty.getEntryListener(), 
            notPretty.getEntryPanel(), notPretty.getEntryToggles());
    }

    /**
     * Listens for {@link ucar.unidata.ui.CheckboxCategoryPanel} updates and
     * stores the current status.
     * 
     * @param topic Topic of interest is {@code "CheckboxCategoryPanel.PanelToggled"}.
     * @param catPanel The object that changed.
     */
    @EventTopicSubscriber(topic="CheckboxCategoryPanel.PanelToggled")
    public void handleCategoryToggle(final String topic, final CheckboxCategoryPanel catPanel) {
        IdvObjectStore store = entryStore.getIdvStore();
        store.put("addepref.category."+catPanel.getCategoryName(), catPanel.isOpen());
    }

    /**
     * Builds the remote server preference panel, using the given 
     * {@link McIdasPreferenceManager}.
     * 
     * @param mcv Reference to the McIDAS-V object; mostly used to control the 
     * server manager GUI. Cannot be {@code null}.
     * 
     * @return An object containing the various components required of a 
     * preference panel.
     */
    public AddePrefConglomeration buildPanel(final McIDASV mcv) {
        Contract.notNull(mcv, "Cannot build a preference panel with a null McIDASV object");
        Map<EntryType, Set<AddeEntry>> entries = 
            entryStore.getVerifiedEntriesByTypes();

        final Map<AddeEntry, JCheckBox> entryToggles = new LinkedHashMap<>();

        final List<CheckboxCategoryPanel> typePanels = arrList();
        List<JPanel> compList = arrList();

        final IdvObjectStore store = mcv.getStore();

        // create checkboxes for each AddeEntry and add 'em to the appropriate 
        // CheckboxCategoryPanel 
        for (final EntryType type : EntryType.values()) {
            if (EntryType.INVALID.equals(type)) {
                continue;
            }
            final Set<AddeEntry> subset = entries.get(type);
            Set<String> observedEntries = new HashSet<>(subset.size());
            boolean typePanelVis = store.get("addepref.category."+type.toString(), false);
            final CheckboxCategoryPanel typePanel = 
                new CheckboxCategoryPanel(type.toString(), typePanelVis);

            for (final AddeEntry entry : subset) {
                final String entryText = entry.getEntryText();
                if (observedEntries.contains(entryText)) {
                    continue;
                }

                boolean enabled = entry.getEntryStatus() == EntryStatus.ENABLED;
                final JCheckBox cbx = new JCheckBox(entryText, enabled);
                cbx.addActionListener(new ActionListener() {
                    public void actionPerformed(final ActionEvent e) {
                        EntryStatus status = cbx.isSelected() ? EntryStatus.ENABLED : EntryStatus.DISABLED;
                        logger.trace("entry={} val={} status={} evt={}", new Object[] { entryText, cbx.isSelected(), status, e});
                        entry.setEntryStatus(status);
                        entryToggles.put(entry, cbx);
                        store.put("addeprefs.scroller.pos", cbScroller.getViewport().getViewPosition());
                        EventBus.publish(EntryStore.Event.UPDATE);
                    }
                });
                entryToggles.put(entry, cbx);
                typePanel.addItem(cbx);
                typePanel.add(LayoutUtil.inset(cbx, new Insets(0, 20, 0, 0)));
                observedEntries.add(entryText);
            }

            compList.add(typePanel.getTopPanel());
            compList.add(typePanel);
            typePanels.add(typePanel);
            typePanel.checkVisCbx();
        }

        // create the basic pref panel
        // TODO(jon): determine dimensions more intelligently!
        cbPanel = GuiUtils.top(GuiUtils.vbox(compList));
        cbScroller = new JScrollPane(cbPanel);
        cbScroller.getVerticalScrollBar().setUnitIncrement(10);
        cbScroller.setPreferredSize(new Dimension(300, 300));
        Point oldPos = (Point)store.get("addeprefs.scroller.pos");
        if (oldPos == null) {
            oldPos = new Point(0,0);
        }
        cbScroller.getViewport().setViewPosition(oldPos);


        // handle the user opting to enable all servers
        allOn = new JButton("All on");
        allOn.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                for (CheckboxCategoryPanel cbcp : typePanels) {
                    cbcp.toggleAll(true);
                }
            }
        });

        // handle the user opting to disable all servers
        allOff = new JButton("All off");
        allOff.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                for (CheckboxCategoryPanel cbcp : typePanels) {
                    cbcp.toggleAll(false);
                }
            }
        });

        // user wants to add a server! make it so.
        final JButton addServer = new JButton("Add ADDE Servers...");
        addServer.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                mcv.showServerManager();
            }
        });

        // import list of servers
        final JButton importServers = new JButton("Import Servers...");
        importServers.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                mcv.showServerManager();
            }
        });

        boolean useAll = false;
        boolean specify = false;
        if (Selection.ALL_ENTRIES.equals(getSpecifyServers())) {
            useAll = true;
        } else {
            specify = true;
        }

        // disable user selection of entries--they're using everything
        final JRadioButton useAllBtn = 
            new JRadioButton("Use all ADDE entries", useAll);
        useAllBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                setGUIEnabled(!useAllBtn.isSelected());
                // TODO(jon): use the eventbus
                setSpecifyServers(Selection.ALL_ENTRIES);
                EventBus.publish(EntryStore.Event.UPDATE); // doesn't work...
            }
        });

        // let the user specify the "active" set, enable entry selection
        final JRadioButton useTheseBtn = 
            new JRadioButton("Use selected ADDE entries:", specify);
        useTheseBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                setGUIEnabled(!useAllBtn.isSelected());
                // TODO(jon): use the eventbus
                setSpecifyServers(Selection.SPECIFIED_ENTRIES);
                EventBus.publish(EntryStore.Event.UPDATE); // doesn't work...
            }
        });
        GuiUtils.buttonGroup(useAllBtn, useTheseBtn);

        // force the selection state
        setGUIEnabled(!useAllBtn.isSelected());

        JPanel widgetPanel =
            LayoutUtil.topCenter(
                LayoutUtil.hbox(useAllBtn, useTheseBtn),
                LayoutUtil.leftCenter(
                    LayoutUtil.inset(
                        LayoutUtil.top(LayoutUtil.vbox(allOn, allOff, addServer, importServers)),
                        4), cbScroller));

        JPanel entryPanel =
            LayoutUtil.topCenter(
                LayoutUtil.inset(
                    new JLabel("Specify the active ADDE servers:"),
                    4), widgetPanel);
        entryPanel = LayoutUtil.inset(LayoutUtil.left(entryPanel), 6);

        // iterate through all the entries and "apply" any changes: 
        // reordering, removing, visibility changes, etc
        PreferenceManager entryListener = new PreferenceManager() {
            public void applyPreference(XmlObjectStore store, Object data) {
//                logger.trace("well, the damn thing fires at least");
//                // this won't break because the data parameter is whatever
//                // has been passed in to "prefManager.add(...)". in this case,
//                // it's the "entryToggles" variable.
                store.put("addeprefs.scroller.pos", cbScroller.getViewport().getViewPosition());
                
                @SuppressWarnings("unchecked")
                Map<AddeEntry, JCheckBox> toggles = (Map<AddeEntry, JCheckBox>)data;
                boolean updated = false;
                for (Entry<AddeEntry, JCheckBox> entry : toggles.entrySet()) {
                    AddeEntry e = entry.getKey();
                    JCheckBox c = entry.getValue();
                    EntryStatus currentStatus = e.getEntryStatus();
                    EntryStatus nextStatus = c.isSelected() ? EntryStatus.ENABLED : EntryStatus.DISABLED;
                    logger.trace("entry={} type={} old={} new={}", e, e.getEntryType(), currentStatus, nextStatus);
//                    if (currentStatus != nextStatus) {
                        e.setEntryStatus(nextStatus);
                        toggles.put(e, c);
                        updated = true;
//                    }
                }
                if (updated) {
                    EventBus.publish(EntryStore.Event.UPDATE);
                }
            }
        };
        return new AddePrefConglomeration(Constants.PREF_LIST_ADDE_SERVERS, entryListener, entryPanel, entryToggles);
    }

    /**
     * Enables or disables:<ul>
     * <li>{@link JPanel} containing the {@link AddeEntry}s ({@link #cbPanel}).</li>
     * <li>{@link JButton} that enables all available {@code AddeEntry}s ({@link #allOn}).</li>
     * <li>{@code JButton} that disables all available {@code AddeEntry}s ({@link #allOff}).</li>
     * </ul>
     * Enabling the components allows the user to pick and choose servers, while
     * disabling enables all servers.
     * 
     * @param enabled {@code true} enables the components and {@code false} disables.
     */
    public void setGUIEnabled(final boolean enabled) {
        if (cbPanel != null) {
            GuiUtils.enableTree(cbPanel, enabled);
        }
        if (allOn != null) {
            GuiUtils.enableTree(allOn, enabled);
        }
        if (allOff != null) {
            GuiUtils.enableTree(allOff, enabled);
        }
    }

    /**
     * Sets the value of the {@link #PREF_LIST_SPECIFY} preference to 
     * {@code value}. 
     * 
     * @param entrySelection New value to associate with {@code PREF_LIST_SPECIFY}.
     */
    private void setSpecifyServers(final Selection entrySelection) {
        entryStore.getIdvStore().put(PREF_LIST_SPECIFY, entrySelection.toString());
    }

    /**
     * Returns the value of the {@link #PREF_LIST_SPECIFY} preference. Defaults
     * to {@literal "ALL"}.
     */
    private Selection getSpecifyServers() {
        String saved = entryStore.getIdvStore().get(PREF_LIST_SPECIFY, Selection.ALL_ENTRIES.toString());
        Selection entrySelection;
        if ("ALL".equalsIgnoreCase(saved)) {
            entrySelection = Selection.ALL_ENTRIES;
        } else if ("SPECIFY".equalsIgnoreCase(saved)) {
            entrySelection = Selection.SPECIFIED_ENTRIES;
        } else {
            entrySelection = Selection.valueOf(saved);
        }
        
        return entrySelection;
    }

    /**
     * This class is essentially a specialized tuple of the different things 
     * required by the {@link ucar.unidata.idv.IdvPreferenceManager}.
     */
    public static class AddePrefConglomeration {
        private final String name;
        private final PreferenceManager entryListener;
        private final JPanel entryPanel;
        private final Map<AddeEntry, JCheckBox> entryToggles;
        public AddePrefConglomeration(String name, PreferenceManager entryListener, JPanel entryPanel, Map<AddeEntry, JCheckBox> entryToggles) {
            this.name = name;
            this.entryListener = entryListener;
            this.entryPanel = entryPanel;
            this.entryToggles = entryToggles;
        }
        public String getName() { return name; }
        public PreferenceManager getEntryListener() { return entryListener; }
        public JPanel getEntryPanel() { return entryPanel; }
        public Map<AddeEntry, JCheckBox> getEntryToggles() { return entryToggles; }
    }
}
