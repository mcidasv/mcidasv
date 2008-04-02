package edu.wisc.ssec.mcidasv.ui;

import java.util.List;

import javax.swing.JComponent;

import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.ui.IdvComponentHolder;
import ucar.unidata.idv.ViewManager;

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

	/** Kept around to avoid annoying casting. */
	private UIManager uiManager;

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
	 * <p>Merely sets the name of this component holder to the contents of 
	 * <tt>value</tt>.</p>
	 * 
	 * <p>Overridden so that McV can tell the ViewPanel to update upon a name
	 * change.</p>
	 * 
	 * @param value The new name of this component holder.
	 */
	@Override
	public void setName(String value) {
		super.setName(value);

		List<ViewManager> vms = getViewManagers();
		if (vms != null) {
			for (int i = 0; i < vms.size(); i++)
				uiManager.getViewPanel().viewManagerChanged(vms.get(i));
		}
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
	@Override
	protected JComponent makeSkin() {
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
}
