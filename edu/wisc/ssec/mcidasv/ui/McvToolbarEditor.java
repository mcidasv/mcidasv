package edu.wisc.ssec.mcidasv.ui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ucar.unidata.idv.IdvResourceManager;
import ucar.unidata.idv.PluginManager;
import ucar.unidata.ui.TwoListPanel;
import ucar.unidata.ui.XmlUi;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;

/**
 * 
 */
public class McvToolbarEditor implements ActionListener {

    private static final String MENU_OVERWRITE = "Select this if you want to replace the selected menu with the new menu.";

    private static final String MENU_PLUGINEXPORT = "Export to Menu Plugin";

    private static final String MENU_ENTERNAME = "Please enter a menu name";

    private static final String SELECT_ENTRIES = 
        "Please select entries in the Toolbar list";

    /** Add a &quot;space&quot; entry */
    private static final String CMD_ADDSPACE = "Add Space";

    /** Action command for reloading the toolbar list with the original items */
    private static final String CMD_RELOAD = "Reload Original";

    /** action command */
    private static final String CMD_EXPORTPLUGIN = "Export Selected to Plugin";

    /** action command */
    private static final String CMD_EXPORTMENUPLUGIN =
        "Export Selected to Menu Plugin";

    private static final String TT_EXPORT_SELECT = "Export the selected items to the plugin";
    private static final String TT_EXPORT_SELECTMENU = "Export the selected items as a menu to the plugin";

    /** For adding a space */
    private static final String SPACE = "-space-";

    /** Gives us unique ids for the space objects */
    private int spaceCount = 0;

    /** The ui manager */
    private UIManager uiManager;

    /** The gui contents */
    private JComponent contents;

    /** Does the real work */
    private TwoListPanel twoListPanel;

    /** The toolbar xml resources */
    XmlResourceCollection resources;

    /** used to export toolbars to plugin */
    private JTextField menuNameFld;

    /** used to export toolbars to plugin */
    private JComboBox menuIdBox;

    /** used to export toolbars to plugin */
    private JCheckBox menuOverwriteCbx;

    /**
     * The ctor
     *
     * @param uiManager The UI Manager
     */
    public McvToolbarEditor(final UIManager mngr) {
        uiManager = mngr;
        resources = mngr.getIdv().getResourceManager().getXmlResources(
            IdvResourceManager.RSC_TOOLBAR);
        init();
    }

    /**
     * @return Whether or not the given TwoFacedObject represents a space.
     */
    public static boolean isSpace(final TwoFacedObject tfo) {
        return tfo.toString().equals(SPACE);
    }

    /**
     * @return Current toolbar contents as action IDs mapped to labels.
     */
    private List<TwoFacedObject> getCurrentToolbar() {
        List<TwoFacedObject> icons = new ArrayList<TwoFacedObject>();
        List<String> currentIcons = uiManager.getCachedButtons();
        Map<String, String[]> allActions = uiManager.getCachedActions();

        // remember, a null ID signifies a "space!"
        for (String id : currentIcons) {
            TwoFacedObject tfo;
            if (id != null) {
                String label = allActions.get(id)[1];
                tfo = new TwoFacedObject(label, id);
            } else {
                tfo = new TwoFacedObject(SPACE, SPACE + (spaceCount++));
            }
            icons.add(tfo);
        }
        return icons;
    }

    /**
     * @return All the actions as TwoFacedObject-s
     */
    private List<TwoFacedObject> getAllActions() {
        Map<String, String[]> allActions = uiManager.getCachedActions();
        List<TwoFacedObject> actions = new ArrayList<TwoFacedObject>();

        for (String id : allActions.keySet()) {
            String label = allActions.get(id)[1];
            if (label == null)
                label = id;
            actions.add(new TwoFacedObject(label, id));
        }
        return actions;
    }

    public TwoListPanel getTLP() {
        return twoListPanel;
    }

    /**
     * @return The GUI contents
     */
    public JComponent getContents() {
        return contents;
    }

    /**
     * Initialize the editor window contents.
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

        List<JComponent> buttons = new ArrayList<JComponent>(); 
        buttons.add(new JLabel(" "));
        buttons.add(addSpaceButton);
        buttons.add(reloadButton);
        buttons.add(new JLabel(" "));
        buttons.add(export1Button);
        buttons.add(export2Button);

        JPanel extra = GuiUtils.vbox(buttons);

        twoListPanel =
            new TwoListPanel(actions, "Actions", currentIcons, "Toolbar", extra);

        contents = GuiUtils.centerBottom(twoListPanel, new JLabel(" "));
    }

    /**
     * Export the selected actions as a menu to the plugin manager
     *
     * @param tfos selected actions
     */
    private void doExportToMenu(Object[] tfos) {
        if (menuNameFld == null) {
            menuNameFld = new JTextField("", 10);

            Map<String, JMenu> menuIds = uiManager.getMenuIds();

            Vector<TwoFacedObject> menuIdItems = new Vector<TwoFacedObject>();
            menuIdItems.add(new TwoFacedObject("None", null));

            for (String id : menuIds.keySet()) {
                JMenu menu = menuIds.get(id);
                menuIdItems.add(new TwoFacedObject(menu.getText(), id));
            }

            menuIdBox = new JComboBox(menuIdItems);
            menuOverwriteCbx = new JCheckBox("Overwrite", false);
            menuOverwriteCbx.setToolTipText(MENU_OVERWRITE);
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
            if (menuName.length() == 0) {
                LogUtil.userMessage(MENU_ENTERNAME);
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
            for (int i = 0; i < tfos.length; i++) {
                TwoFacedObject tfo = (TwoFacedObject)tfos[i];
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
    private void doExport(Object[] tfos) {
        StringBuffer content = new StringBuffer();
        for (int i = 0; i < tfos.length; i++) {
            TwoFacedObject tfo = (TwoFacedObject) tfos[i];
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
     * Handle the action
     *
     * @param ae The action
     */
    public void actionPerformed(ActionEvent ae) {
        String c = ae.getActionCommand();
        if (c.equals(CMD_EXPORTMENUPLUGIN) || c.equals(CMD_EXPORTPLUGIN)) {
            Object[] tfos = twoListPanel.getToList().getSelectedValues();
            if (tfos.length == 0)
                LogUtil.userMessage(SELECT_ENTRIES);
            else if (c.equals(CMD_EXPORTMENUPLUGIN))
                doExportToMenu(tfos);
            else
                doExport(tfos);
        }
        else if (c.equals(CMD_RELOAD)) {
            twoListPanel.reload();
        } 
        else if (c.equals(CMD_ADDSPACE)) {
            twoListPanel.insertEntry(
                new TwoFacedObject(SPACE, SPACE+(spaceCount++)));
        }
    }

    /**
     * Were there any changes
     *
     * @return Any changes
     */
    public boolean anyChanges() {
        return twoListPanel.getChanged();
    }

    /**
     * Write out the toolbar xml.
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
     * Sorts a {@link java.util.List} of 
     * {@link ucar.unidata.util.TwoFacedObject}s by label. Case is ignored.
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
}

