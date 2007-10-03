package edu.wisc.ssec.mcidasv.data;

import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.idv.IdvProjectionManager;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.view.geoloc.ProjectionManager;

public class McIDASVProjectionManager extends IdvProjectionManager {

	/**
	 * Register the <tt>McIDASVLatLonProjection</tt> and pass to the super.
	 * @param idv
	 */
	public McIDASVProjectionManager(IntegratedDataViewer idv) {
		super(idv);
	    ProjectionManager.addDefaultProjection("edu.wisc.ssec.mcidasv.data.McIDASVLatLonProjection");
	}

    /**
     * Make the default display projection a <tt>McIDASVLatLonProjection</tt>
     * @return Default display projection
     * @see edu.wisc.ssec.mcidasv.data.McIDASVProjectionManager
     */
	@Override
    protected ProjectionImpl makeDefaultProjection() {
        return new McIDASVLatLonProjection("World",
                                    new ProjectionRect(-180., -180., 180.,
                                        180.));
    }
	
}
