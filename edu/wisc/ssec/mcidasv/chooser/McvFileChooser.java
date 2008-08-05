package edu.wisc.ssec.mcidasv.chooser;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JComboBox;

import org.w3c.dom.Element;

import ucar.unidata.idv.chooser.FileChooser;
import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

/**
 * {@code McvFileChooser} is another {@literal "UI nicety"} extension. The main
 * difference is that this class allows {@code choosers.xml} to specify a
 * boolean attribute, {@code "selectdatasourceid"}. If disabled or not present,
 * a {@code McvFileChooser} will behave exactly like a standard 
 * {@link FileChooser}.
 * 
 * <p>If the attribute is present and enabled, the {@code McvFileChooser}'s 
 * data source type will automatically select the 
 * {@link ucar.unidata.data.DataSource} corresponding to the chooser's 
 * {@code "datasourceid"} attribute.
 */
public class McvFileChooser extends FileChooser {

    /** 
     * Chooser attribute that controls selecting the default data source.
     * @see #selectDefaultDataSource
     */
    public static final String ATTR_SELECT_DSID = "selectdatasourceid";

    /** Default data source ID for this chooser. Defaults to {@code null}. */
    private final String defaultDataSourceId;

    /** 
     * Whether or not to select the data source corresponding to 
     * {@link #defaultDataSourceId} within the {@link JComboBox} returned by
     * {@link #getDataSourcesComponent()}. Defaults to {@code false}.
     */
    private final boolean selectDefaultDataSource; 

    /**
     * Creates a {@code McvFileChooser} and bubbles up {@code mgr} and 
     * {@code root} to {@link FileChooser}.
     * 
     * @param mgr Global IDV chooser manager.
     * @param root XML representing this chooser.
     */
    public McvFileChooser(final IdvChooserManager mgr, final Element root) {
        super(mgr, root);

        String id = XmlUtil.getAttribute(root, ATTR_DATASOURCEID, (String)null);
        defaultDataSourceId = (id != null) ? id.toLowerCase() : id;

        selectDefaultDataSource =
            XmlUtil.getAttribute(root, ATTR_SELECT_DSID, false);
    }

    /**
     * Overridden so that McIDAS-V can attempt auto-selecting the default data
     * source type.
     */
    @Override protected JComboBox getDataSourcesComponent() {
        JComboBox comboBox = getDataSourcesComponent(true);
        if (selectDefaultDataSource && defaultDataSourceId != null) {
            Map<String, Integer> ids = comboBoxContents(comboBox);
            if (ids.containsKey(defaultDataSourceId))
                comboBox.setSelectedIndex(ids.get(defaultDataSourceId));
        }
        return comboBox;
    }

    /**
     * Maps data source IDs to their index within {@code box}. This method is 
     * only applicable to {@link JComboBox}es created for {@link FileChooser}s.
     * 
     * @param box Combo box containing relevant data source IDs and indices. 
     * 
     * @return A mapping of data source IDs to their offset within {@code box}.
     */
    private static Map<String, Integer> comboBoxContents(final JComboBox box) {
        assert box != null;
        Map<String, Integer> map = new HashMap<String, Integer>();
        for (int i = 0; i < box.getItemCount(); i++) {
            Object o = box.getItemAt(i);
            if (!(o instanceof TwoFacedObject))
                continue;
            TwoFacedObject tfo = (TwoFacedObject)o;
            map.put(TwoFacedObject.getIdString(tfo), i);
        }
        return map;
    }
}