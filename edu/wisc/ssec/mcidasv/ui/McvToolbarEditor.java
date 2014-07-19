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

package edu.wisc.ssec.mcidasv.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import edu.wisc.ssec.mcidasv.ui.UIManager.ActionAttribute;
import edu.wisc.ssec.mcidasv.ui.UIManager.IdvActions;

import ucar.unidata.idv.IdvResourceManager;
import ucar.unidata.idv.PluginManager;
import ucar.unidata.ui.TwoListPanel;
import ucar.unidata.ui.XmlUi;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;

public class McvToolbarEditor implements ActionListener {

    /** Size of the icons to be shown in the {@link TwoListPanel}. */
    protected static final int ICON_SIZE = 16;

    private static final String MENU_PLUGINEXPORT = "Export to Menu Plugin";

    private static final String MSG_ENTER_NAME = "Please enter a menu name";

    private static final String MSG_SELECT_ENTRIES = 
        "Please select entries in the Toolbar list";

    /** Add a &quot;space&quot; entry */
    private static final String CMD_ADDSPACE = "Add Space";

    /** Action command for reloading the toolbar list with original items */
    private static final String CMD_RELOAD = "Reload Original";

    /** action command */
    private static final String CMD_EXPORTPLUGIN = "Export Selected to Plugin";

    /** action command */
    private static final String CMD_EXPORTMENUPLUGIN =
        "Export Selected to Menu Plugin";

    /** */
    private static final String TT_EXPORT_SELECT = 
        "Export the selected items to the plugin";

    private static final String TT_EXPORT_SELECTMENU = 
        "Export the selected items as a menu to the plugin";

    private static final String TT_OVERWRITE = 
        "Select this if you want to replace the selected menu with the new" +
        "menu.";

    /** ID that represents a &quot;space&quot; in the toolbar. */
    private static final String SPACE = "-space-";

    /** Provides simple IDs for the space entries. */
    private int spaceCount = 0;

    /** Used to notify the application that a toolbar update should occur. */
    private UIManager uiManager;

    /** All of the toolbar editor's GUI components. */
    private JComponent contents;

    /** The GUI component that stores both available and selected actions. */
    private TwoListPanel twoListPanel;

    /** The toolbar XML resources. */
    XmlResourceCollection resources;

    /** Used to export toolbars to plugin. */
    private JTextField menuNameFld;

    /** Used to export toolbars to plugin. */
    private JComboBox menuIdBox;

    /** Used to export toolbars to plugin. */
    private JCheckBox menuOverwriteCbx;

    /**
     * Builds a toolbar editor and associates it with the {@link UIManager}.
     *
     * @param mngr The application's UI Manager.
     */
    public McvToolbarEditor(final UIManager mngr) {
        uiManager = mngr;
        resources = mngr.getIdv().getResourceManager().getXmlResources(
            IdvResourceManager.RSC_TOOLBAR);
        init();
    }

    /**
     * Returns the icon associated with {@code actionId}.
     */
    protected Icon getActionIcon(final String actionId) {
        return uiManager.getActionIcon(actionId, UIManager.ToolbarStyle.SMALL);
    }

    /**
     * Determines if a given toolbar entry (in the form of a 
     * {@link ucar.unidata.util.TwoFacedObject}) represents a space.
     * 
     * @param tfo The entry to test.
     * 
     * @return Whether or not the entry represents a space.
     */
    public static boolean isSpace(final TwoFacedObject tfo) {
        return SPACE.equals(tfo.toString());
    }

    /**
     * @return Current toolbar contents as action IDs mapped to labels.
     */
    private List<TwoFacedObject> getCurrentToolbar() {

        List<String> currentIcons = uiManager.getCachedButtons();
        IdvActions allActions = uiManager.getCachedActions();
        List<TwoFacedObject> icons = new ArrayList<>(currentIcons.size());
        for (String actionId : currentIcons) {
            TwoFacedObject tfo;
            if (actionId != null) {
                String desc = allActions.getAttributeForAction(actionId, ActionAttribute.DESCRIPTION);
                if (desc == null) {
                    desc = "No description associated with action \""+actionId+"\"";
                }
                tfo = new TwoFacedObject(desc, actionId);
            } else {
                tfo = new TwoFacedObject(SPACE, SPACE + (spaceCount++));
            }
            icons.add(tfo);
        }
        return icons;
    }

    /**
     * Returns a {@link List} of {@link TwoFacedObject}s containing all of the
     * actions known to McIDAS-V.
     */
    private List<TwoFacedObject> getAllActions() {
        IdvActions allActions = uiManager.getCachedActions();
        List<String> actionIds = allActions.getAttributes(ActionAttribute.ID);
        List<TwoFacedObject> actions = new ArrayList<>(actionIds.size());
        for (String actionId : actionIds) {
            String label = allActions.getAttributeForAction(actionId, ActionAttribute.DESCRIPTION);
            if (label == null) {
                label = actionId;
            }
            actions.add(new TwoFacedObject(label, actionId));
        }
        return actions;
    }

    /**
     * Returns the {@link TwoListPanel} being used to store the lists of
     * available and selected actions.
     */
    public TwoListPanel getTLP() {
        return twoListPanel;
    }

    /**
     * Returns the {@link JComponent} that contains all of the toolbar editor's
     * UI components.
     */
    public JComponent getContents() {
        return contents;
    }

    /**
     * Initializes the editor window contents.
     */
    private void init() {
        List<TwoFacedObject> currentIcons = getCurrentToolbar();
        List<TwoFacedObject> actions = sortTwoFaced(getAllActions());

        JButton addSpaceButton = new JButton("Add space");
        addSpaceButton.setActionCommand(CMD_ADDSPACE);
        addSpaceButton.addActionListener(this);

        JButton reloadButton = new JButton(CMD_RELOAD);
        reloadButton.setActionCommand(CMD_RELOAD);
        reloadButton.addActionListener(this);

        JButton export1Button = new JButton(CMD_EXPORTPLUGIN);
        export1Button.setToolTipText(TT_EXPORT_SELECT);
        export1Button.setActionCommand(CMD_EXPORTPLUGIN);
        export1Button.addActionListener(this);

        JButton export2Button = new JButton(CMD_EXPORTMENUPLUGIN);
        export2Button.setToolTipText(TT_EXPORT_SELECTMENU);
        export2Button.setActionCommand(CMD_EXPORTMENUPLUGIN);
        export2Button.addActionListener(this);

        List<JComponent> buttons = new ArrayList<>(12);
        buttons.add(new JLabel(" "));
        buttons.add(addSpaceButton);
        buttons.add(reloadButton);
        buttons.add(new JLabel(" "));
        buttons.add(export1Button);
        buttons.add(export2Button);

        JPanel extra = GuiUtils.vbox(buttons);

        twoListPanel =
            new TwoListPanel(actions, "Actions", currentIcons, "Toolbar", extra);

        ListCellRenderer renderer = new IconCellRenderer(this);
        twoListPanel.getToList().setCellRenderer(renderer);
        twoListPanel.getFromList().setCellRenderer(renderer);

        contents = GuiUtils.centerBottom(twoListPanel, new JLabel(" "));
    }

    /**
     * Export the selected actions as a menu to the plugin manager.
     *
     * @param tfos selected actions
     */
    private void doExportToMenu(List<Object> tfos) {
        if (menuNameFld == null) {
            menuNameFld = new JTextField("", 10);

            Map<String, JMenu> menuIds = uiManager.getMenuIds();

            Vector<TwoFacedObject> menuIdItems = new Vector<>();
            menuIdItems.add(new TwoFacedObject("None", null));

            for (String id : menuIds.keySet()) {
                JMenu menu = menuIds.get(id);
                menuIdItems.add(new TwoFacedObject(menu.getText(), id));
            }

            menuIdBox = new JComboBox(menuIdItems);
            menuOverwriteCbx = new JCheckBox("Overwrite", false);
            menuOverwriteCbx.setToolTipText(TT_OVERWRITE);
        }

        GuiUtils.tmpInsets = GuiUtils.INSETS_5;
        JComponent dialogContents = GuiUtils.doLayout(new Component[] {
                                        GuiUtils.rLabel("Menu Name:"),
                                        menuNameFld,
                                        GuiUtils.rLabel("Add to Menu:"),
                                        GuiUtils.left(
                                            GuiUtils.hbox(
                                                menuIdBox,
                                                menuOverwriteCbx)) }, 2,
                                                    GuiUtils.WT_NY,
                                                    GuiUtils.WT_N);
        PluginManager pluginManager = uiManager.getIdv().getPluginManager();
        while (true) {
            if (!GuiUtils.askOkCancel(MENU_PLUGINEXPORT, dialogContents)) {
                return;
            }

            String menuName = menuNameFld.getText().trim();
            if (menuName.isEmpty()) {
                LogUtil.userMessage(MSG_ENTER_NAME);
                continue;
            }

            StringBuffer xml = new StringBuffer();
            xml.append(XmlUtil.XML_HEADER);
            String idXml = "";

            TwoFacedObject menuIdTfo = 
                (TwoFacedObject)menuIdBox.getSelectedItem();

            if (menuIdTfo.getId() != null) {
                idXml = XmlUtil.attr("id", menuIdTfo.getId().toString());
                if (menuOverwriteCbx.isSelected())
                    idXml = idXml + XmlUtil.attr("replace", "true");
            }

            xml.append("<menus>\n");
            xml.append("<menu label=\"" + menuName + "\" " + idXml + ">\n");
            for (int i = 0; i < tfos.size(); i++) {
                TwoFacedObject tfo = (TwoFacedObject)tfos.get(i);
                if (isSpace(tfo)) {
                    xml.append("<separator/>\n");
                } else {
                    xml.append(
                        XmlUtil.tag(
                            "menuitem",
                            XmlUtil.attrs(
                                "label", tfo.toString(), "action",
                                "action:" + tfo.getId().toString())));
                }
            }
            xml.append("</menu></menus>\n");
            pluginManager.addText(xml.toString(), "menubar.xml");
            return;
        }
    }

    /**
     * Export the actions
     *
     * @param tfos the actions
     */
    private void doExport(List<Object> tfos) {
        StringBuffer content = new StringBuffer();
        for (int i = 0; i < tfos.size(); i++) {
            TwoFacedObject tfo = (TwoFacedObject)tfos.get(i);
            if (tfo.toString().equals(SPACE)) {
                content.append("<filler/>\n");
            } else {
                content.append(
                    XmlUtil.tag(
                        "button",
                        XmlUtil.attr(
                            "action", "action:" + tfo.getId().toString())));
            }
        }
        StringBuffer xml = new StringBuffer();
        xml.append(XmlUtil.XML_HEADER);
        xml.append(
            XmlUtil.tag(
                "panel",
                XmlUtil.attrs("layout", "flow", "margin", "4", "vspace", "0")
                + XmlUtil.attrs(
                    "hspace", "2", "i:space", "2", "i:width",
                    "5"), content.toString()));
        LogUtil.userMessage(
            "Note, if a user has changed their toolbar the plugin toolbar will be ignored");
        uiManager.getIdv().getPluginManager().addText(xml.toString(),
                "toolbar.xml");
    }

    /**
     * Handles events such as exporting plugins, reloading contents, and adding
     * spaces.
     * 
     * @param ae The event that invoked this method.
     */
    public void actionPerformed(ActionEvent ae) {
        String c = ae.getActionCommand();
        if (CMD_EXPORTMENUPLUGIN.equals(c) || CMD_EXPORTPLUGIN.equals(c)) {
            List<Object> tfos = twoListPanel.getToList().getSelectedValuesList();
            if (tfos.isEmpty()) {
                LogUtil.userErrorMessage(MSG_SELECT_ENTRIES);
            } else if (CMD_EXPORTMENUPLUGIN.equals(c)) {
                doExportToMenu(tfos);
            } else {
                doExport(tfos);
            }
        } else if (CMD_RELOAD.equals(c)) {
            twoListPanel.reload();
        } else if (CMD_ADDSPACE.equals(c)) {
            twoListPanel.insertEntry(
                new TwoFacedObject(SPACE, SPACE+(spaceCount++)));
        }
    }

    /**
     * Has {@code twoListPanel} been changed?
     *
     * @return {@code true} if there have been changes, {@code false}
     * otherwise.
     */
    public boolean anyChanges() {
        return twoListPanel.getChanged();
    }

    /**
     * Writes out the toolbar xml.
     */
    public void doApply() {
        Document doc  = resources.getWritableDocument("<panel/>");
        Element  root = resources.getWritableRoot("<panel/>");
        root.setAttribute(XmlUi.ATTR_LAYOUT, XmlUi.LAYOUT_FLOW);
        root.setAttribute(XmlUi.ATTR_MARGIN, "4");
        root.setAttribute(XmlUi.ATTR_VSPACE, "0");
        root.setAttribute(XmlUi.ATTR_HSPACE, "2");
        root.setAttribute(XmlUi.inheritName(XmlUi.ATTR_SPACE), "2");
        root.setAttribute(XmlUi.inheritName(XmlUi.ATTR_WIDTH), "5");

        XmlUtil.removeChildren(root);
        List<TwoFacedObject> icons = twoListPanel.getCurrentEntries();
        for (TwoFacedObject tfo : icons) {
            Element element;
            if (isSpace(tfo)) {
                element = doc.createElement(XmlUi.TAG_FILLER);
                element.setAttribute(XmlUi.ATTR_WIDTH, "5");
            } else {
                element = doc.createElement(XmlUi.TAG_BUTTON);
                element.setAttribute(XmlUi.ATTR_ACTION,
                                     "action:" + tfo.getId().toString());
            }
            root.appendChild(element);
        }
        try {
            resources.writeWritable();
        } catch (Exception exc) {
            LogUtil.logException("Writing toolbar", exc);
        }
    }

    /**
     * <p>
     * Sorts a {@link List} of 
     * {@link TwoFacedObject}s by label. Case is ignored.
     * </p>
     * 
     * @param objs The list that needs some sortin' out.
     * 
     * @return The sorted contents of <tt>objs</tt>.
     */
    private List<TwoFacedObject> sortTwoFaced(final List<TwoFacedObject> objs) {
        Comparator<TwoFacedObject> comp = new Comparator<TwoFacedObject>() {
            public int compare(final TwoFacedObject a, final TwoFacedObject b) {
                return ((String)a.getLabel()).compareToIgnoreCase((String)b.getLabel());
            }
        };

        List<TwoFacedObject> reordered = new ArrayList<TwoFacedObject>(objs);
        Collections.sort(reordered, comp);
        return reordered;
    }

    /**
     * Renders a toolbar action and its icon within the {@link TwoListPanel}'s 
     * {@link JList}s.
     */
    private static class IconCellRenderer implements ListCellRenderer {

        /** Icon that represents spaces in the current toolbar actions. */
        private static final Icon SPACE_ICON = 
            new SpaceIcon(McvToolbarEditor.ICON_SIZE);

        /** Used to capture the normal cell renderer behaviors. */
        private DefaultListCellRenderer defaultRenderer = 
            new DefaultListCellRenderer();

        /** Used to determine the action ID to icon associations. */
        private McvToolbarEditor editor;

        /**
         * Associates this renderer with the {@link McvToolbarEditor} that
         * created it.
         * 
         * @param editor Toolbar editor that contains relevant action ID to 
         * icon mapping.
         * 
         * @throws NullPointerException if a null McvToolbarEditor was given.
         */
        public IconCellRenderer(final McvToolbarEditor editor) {
            if (editor == null) {
                throw new NullPointerException("Toolbar editor cannot be null");
            }
            this.editor = editor;
        }

        // draws the icon associated with the action ID in value next to the
        // text label.
        public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus) 
        {
            JLabel renderer = 
                (JLabel)defaultRenderer.getListCellRendererComponent(list, 
                    value, index, isSelected, cellHasFocus);

            if (value instanceof TwoFacedObject) {
                TwoFacedObject tfo = (TwoFacedObject)value;
                String text = (String)tfo.getLabel();
                Icon icon;
                if (!isSpace(tfo)) {
                    icon = editor.getActionIcon((String)tfo.getId());
                } else {
                    icon = SPACE_ICON;
                }
                renderer.setIcon(icon);
                renderer.setText(text);
            }
            return renderer;
        }
    }

    /**
     * {@code SpaceIcon} is a class that represents a {@literal "space"} entry
     * in the {@link TwoListPanel} that holds the current toolbar actions.
     * 
     * <p>Probably only of use in {@link IconCellRenderer}.
     */
    private static class SpaceIcon implements Icon {

        /** {@code dimension * dimension} is the size of the icon. */
        private final int dimension;

        /** 
         * Creates a blank, square icon whose dimensions are {@code dimension} 
         * 
         * @param dimension Icon dimensions.
         * 
         * @throws IllegalArgumentException if dimension is less than or equal 
         * zero.
         */
        public SpaceIcon(final int dimension) {
            if (dimension <= 0) {
                throw new IllegalArgumentException("Dimension must be a positive integer");
            }
            this.dimension = dimension;
        }

        public int getIconHeight() {
            return dimension;
        }

        public int getIconWidth() {
            return dimension;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(new Color(255, 255, 255, 0));
            g.drawRect(0, 0, dimension, dimension);
        }
    }
}

