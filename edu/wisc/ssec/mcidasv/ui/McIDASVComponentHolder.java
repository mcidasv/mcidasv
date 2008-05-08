package edu.wisc.ssec.mcidasv.ui;

import java.util.List;

import javax.swing.JComponent;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.ui.IdvComponentHolder;
import ucar.unidata.idv.ui.IdvUIManager;
import ucar.unidata.idv.ui.IdvXmlUi;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.util.WrapperException;
import ucar.unidata.xml.XmlUtil;

/**
 * <p>McIDAS-V needs its own ComponentHolder merely to associate ViewManagers 
 * with their parent ComponentHolders. This association is later used in 
 * McIDASVViewPanel to create a "hierarchical name" for each ViewManager.</p>
 * 
 * <p>Instead of having something like "Panel 1" appearing in the layer 
 * controls, we now have "ComponentHolder Name>Panel 1". Note: ComponentHolder 
 * names always double as tab names! McV also intercepts ComponentHolder 
 * renaming and updates the layer controls instantly.</p>
 */
public class McIDASVComponentHolder extends IdvComponentHolder {

	/** IDV friendly description of a dynamic XML skin. */
	public static final String CATEGORY_DESCRIPTION = "UI Skin";

	/** Used to distinguish a dynamic skin from other things. */
	public static final String TYPE_DYNAMIC_SKIN = "dynamicskin";

	/** Kept around to avoid annoying casting. */
	private UIManager uiManager;

	/**
	 * Default constructor for serialization.
	 */
	public McIDASVComponentHolder() {}

	/**
	 * Fairly typical constructor.
	 * 
	 * @param idv Reference to the main IDV object.
	 * @param obj The object being held in this component holder.
	 */
	public McIDASVComponentHolder(IntegratedDataViewer idv, Object obj) {
		super(idv, obj);
		uiManager = (UIManager)idv.getIdvUIManager();
	}

	/**
	 * Overridden so that we can (one day) do the required extra work to write
	 * out the XML for this skin.
	 * 
	 * @param doc The parent document we'll use for XML generation.
	 * 
	 * @return The XML representation of what is being held.
	 */
	@Override public Element createXmlNode(Document doc) {
		if (!getType().equals(TYPE_DYNAMIC_SKIN))
			return super.createXmlNode(doc);

		// keep in mind that the IDV expects that we're holding a path
		// to a skin... I don't think that this will work how you want it...
		// TODO: investigate this!
		Element node = doc.createElement(IdvUIManager.COMP_COMPONENT_SKIN);
		node.setAttribute("url", getObject().toString());

		/*try {
			System.err.println(XmlUtil.toString((Element)getObject()));
		} catch (Exception e) {
			e.printStackTrace();
		}*/

		return node;
	}

	/**
	 * Overridden so that McV can do the required extra work if this holder is
	 * holding a dynamic XML skin.
	 * 
	 * @return The contents of this holder as a UI component.
	 */
	@Override public JComponent doMakeContents() {
		if (!getType().equals(TYPE_DYNAMIC_SKIN))
			return super.doMakeContents();

		return makeDynamicSkin();
	}

	/**
	 * Lets the IDV take care of the details, but does null out the local 
	 * reference to the UIManager.
	 */
	@Override public void doRemove() {
		super.doRemove();
		uiManager = null;
	}

	/**
	 * Overridden so that McV can return a more accurate category if this 
	 * holder is holding a dynamic skin.
	 * 
	 * @return Category name for the type of thing we're holding.
	 */
	@Override public String getCategory() {
		if (!getType().equals(TYPE_DYNAMIC_SKIN))
			return super.getCategory();

		return CATEGORY_DESCRIPTION;
	}

	/**
	 * Overridden so that McV can return a more accurate description if this
	 * holder is holding a dynamic skin.
	 * 
	 * @return The description of what is being held.
	 */
	@Override public String getTypeName() {
		if (!getType().equals(TYPE_DYNAMIC_SKIN))
			return super.getTypeName();

		return CATEGORY_DESCRIPTION;
	}

	/**
	 * <p>If the object being held in this component holder is a skin, calling 
	 * this method will create a component based upon the skin.</p>
	 * 
	 * <p>Overridden so that McV can tell the UIManager to associate the skin's 
	 * ViewManagers with this component holder. That association is used to 
	 * build the hierarchical names in the ViewPanel.</p>
	 * 
	 * @return The component represented by this holder's skin.
	 */
	@Override protected JComponent makeSkin() {
		JComponent comp = super.makeSkin();

		List<ViewManager> vms = getViewManagers();
		if (vms != null) {
			for (int i = 0; i < vms.size(); i++) {
				uiManager.setViewManagerHolder(vms.get(i), this);
				uiManager.getViewPanel().viewManagerChanged(vms.get(i));
			}
		}

		return comp;
	}

	/**
	 * Mostly used to ensure that the local reference to the UI manager is 
	 * valid when deserializing.
	 * 
	 * @param idv The IDV reference!
	 */
	@Override public void setIdv(IntegratedDataViewer idv) {
		super.setIdv(idv);
		uiManager = (UIManager)idv.getIdvUIManager();
	}

	/**
	 * <p>Merely sets the name of this component holder to the contents of 
	 * <tt>value</tt>.</p>
	 * 
	 * <p>Overridden so that McV can tell the ViewPanel to update upon a name
	 * change.</p>
	 * 
	 * @param value The new name of this component holder.
	 */
	@Override public void setName(String value) {
		super.setName(value);

		List<ViewManager> vms = getViewManagers();
		if (vms != null) {
			for (int i = 0; i < vms.size(); i++)
				uiManager.getViewPanel().viewManagerChanged(vms.get(i));
		}
	}

	/**
	 * Build the UI component using the XML skin contained by this holder.
	 * 
	 * @return UI Component specified by the skin contained in this holder.
	 */
	public JComponent makeDynamicSkin() {
		try {
			Element root = XmlUtil.getRoot((String)getObject());

			IdvXmlUi ui = uiManager.doMakeIdvXmlUi(null, getViewManagers(), root);

			// look for any "embedded" ViewManagers.
			Element startNode = 
				XmlUtil.findElement(root, null, "embeddednode", "true");
			if (startNode != null)
				ui.setStartNode(startNode);

			JComponent contents = (JComponent)ui.getContents();
			setViewManagers(ui.getViewManagers());

			return contents;

		} catch (Exception e) {
			throw new WrapperException(e);
		}
	}

	/**
	 * Tell this component holder's component group that the corresponding tab
	 * should be made the active tab.
	 */
	public void setAsActiveTab() {
		((McIDASVComponentGroup)getParent()).setActiveComponentHolder(this);
	}
}
